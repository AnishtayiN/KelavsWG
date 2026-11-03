package com.kelavs.vpn.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.kelavs.vpn.network.SafeHttpClient
import com.kelavs.vpn.storage.SubscriptionMetadataDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

/**
 * Manages in-app updates from GitHub Releases.
 * Handles version checking, APK download with progress, verification, and installation.
 */
class UpdateManager(
    private val context: Context,
    private val metadataDao: SubscriptionMetadataDao,
    private val scope: CoroutineScope,
    private val githubOwner: String = "AnishtayiN",
    private val githubRepo: String = "KelavsWG",
    private val httpClient: OkHttpClient = SafeHttpClient.createUpdateClient()
) {

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState = _updateState.asStateFlow()

    private val _downloadProgress = MutableStateFlow<DownloadProgress?>(null)
    val downloadProgress = _downloadProgress.asStateFlow()

    private val isDownloading = AtomicBoolean(false)
    private var currentDownloadFile: File? = null

    /**
     * Checks for updates from GitHub Releases.
     * Non-blocking, updates state flow.
     */
    fun checkForUpdates() {
        scope.launch(Dispatchers.IO) {
            _updateState.value = UpdateState.Checking
            
            try {
                val latestRelease = fetchLatestRelease()
                val currentVersion = getCurrentVersion()
                
                if (isNewerVersion(latestRelease.tagName, currentVersion)) {
                    val apkAsset = latestRelease.assets.find { it.name.endsWith(".apk") && it.name.contains("Kelavs-VPN") }
                        ?: throw UpdateException("فایل APK در ریلیز یافت نشد")
                    
                    val checksumAsset = latestRelease.assets.find { it.name.endsWith(".sha256") }
                    
                    _updateState.value = UpdateState.UpdateAvailable(
                        version = latestRelease.tagName,
                        releaseNotes = latestRelease.body ?: "",
                        apkUrl = apkAsset.browserDownloadUrl,
                        apkSize = apkAsset.size,
                        checksumUrl = checksumAsset?.browserDownloadUrl,
                        checksum = null // Will be fetched if URL exists
                    )
                    
                    // Fetch checksum if available
                    checksumAsset?.browserDownloadUrl?.let { url ->
                        scope.launch(Dispatchers.IO) {
                            try {
                                val checksum = fetchChecksum(url)
                                _updateState.value = UpdateState.UpdateAvailable(
                                    version = latestRelease.tagName,
                                    releaseNotes = latestRelease.body ?: "",
                                    apkUrl = apkAsset.browserDownloadUrl,
                                    apkSize = apkAsset.size,
                                    checksumUrl = url,
                                    checksum = checksum
                                )
                            } catch (e: Exception) {
                                // Ignore checksum fetch failure
                            }
                        }
                    }
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: Exception) {
                _updateState.value = UpdateState.Error(e.message ?: "خطای نامشخص")
            }
        }
    }

    /**
     * Downloads the APK with progress reporting.
     */
    fun downloadApk() {
        if (isDownloading.getAndSet(true)) return
        
        scope.launch(Dispatchers.IO) {
            val state = _updateState.value
            val apkUrl = when (state) {
                is UpdateState.UpdateAvailable -> state.apkUrl
                else -> { isDownloading.set(false); return@launch }
            }
            
            val expectedChecksum = when (state) {
                is UpdateState.UpdateAvailable -> state.checksum
                else -> null
            }
            
            try {
                _downloadProgress.value = DownloadProgress(0, 0, 0f, DownloadStatus.Starting)
                
                val request = Request.Builder().url(apkUrl).build()
                val response = httpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw UpdateException("خطای دانلود: ${response.code}")
                }
                
                val totalSize = response.body?.contentLength() ?: -1
                val file = File(context.cacheDir, "kelavs_update_${System.currentTimeMillis()}.apk")
                currentDownloadFile = file
                
                val sink = FileOutputStream(file)
                val source = response.body?.source()
                
                var downloaded = 0L
                val buffer = okio.Buffer()
                
                while (true) {
                    val read = source?.read(buffer, 8192) ?: -1L
                    if (read == -1L) break
                    
                    sink.write(buffer.readByteArray())
                    downloaded += read
                    
                    val progress = if (totalSize > 0) downloaded.toFloat() / totalSize * 100 else 0f
                    _downloadProgress.value = DownloadProgress(
                        downloaded = downloaded,
                        total = totalSize,
                        percentage = progress,
                        status = DownloadStatus.Downloading
                    )
                }
                
                sink.close()
                response.close()
                
                // Verify checksum
                _downloadProgress.value = DownloadProgress(
                    downloaded = downloaded,
                    total = totalSize,
                    percentage = 100f,
                    status = DownloadStatus.Verifying
                )
                
                if (expectedChecksum != null) {
                    val actualChecksum = calculateSha256(file)
                    if (!actualChecksum.equals(expectedChecksum, ignoreCase = true)) {
                        throw UpdateException("Checksum عدم تطابق: فایل دانلود شده خراب است")
                    }
                }
                
                // Verify APK signature
                verifyApkSignature(file)
                
                _downloadProgress.value = DownloadProgress(
                    downloaded = downloaded,
                    total = totalSize,
                    percentage = 100f,
                    status = DownloadStatus.ReadyToInstall
                )
                
                _updateState.value = UpdateState.Downloaded(file)
                
            } catch (e: Exception) {
                _downloadProgress.value = DownloadProgress(
                    downloaded = 0,
                    total = 0,
                    percentage = 0f,
                    status = DownloadStatus.Error(e.message ?: "خطای دانلود")
                )
                _updateState.value = UpdateState.Error(e.message ?: "خطای دانلود")
            } finally {
                isDownloading.set(false)
            }
        }
    }

    /**
     * Installs the downloaded APK using system installer.
     */
    fun installApk() {
        val state = _updateState.value
        val file = when (state) {
            is UpdateState.Downloaded -> state.file
            else -> return
        }
        
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            .setData(uri)
            .setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .putExtra(Intent.EXTRA_RETURN_RESULT, true)
        
        context.startActivity(intent)
    }

    /**
     * Cancels ongoing download.
     */
    fun cancelDownload() {
        if (isDownloading.getAndSet(false)) {
            currentDownloadFile?.delete()
            currentDownloadFile = null
            _downloadProgress.value = null
            _updateState.value = UpdateState.Idle
        }
    }

    private suspend fun fetchLatestRelease(): GitHubRelease {
        val url = "https://api.github.com/repos/$githubOwner/$githubRepo/releases/latest"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "KelavsVPN-UpdateChecker")
            .build()
        
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw UpdateException("GitHub API error: ${response.code}")
            val json = response.body?.string() ?: throw UpdateException("Empty response")
            Json.decodeFromString<GitHubRelease>(json)
        }
    }

    private suspend fun fetchChecksum(url: String): String {
        val request = Request.Builder().url(url).build()
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw UpdateException("Checksum fetch failed: ${response.code}")
            response.body?.string()?.trim()?.split(" ")?[0] ?: throw UpdateException("Invalid checksum format")
        }
    }

    private fun getCurrentVersion(): String = context.packageManager.getPackageInfo(context.packageName, 0).versionName

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.replace("v", "").split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.replace("v", "").split(".").map { it.toIntOrNull() ?: 0 }
        
        for (i in 0..max(latestParts.lastIndex, currentParts.lastIndex)) {
            val l = latestParts.getOrNull(i) ?: 0
            val c = currentParts.getOrNull(i) ?: 0
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val fis = FileInputStream(file)
        val buffer = ByteArray(8192)
        var read: Int
        while (fis.read(buffer).also { read = it } != -1) {
            digest.update(buffer, 0, read)
        }
        fis.close()
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun verifyApkSignature(file: File) {
        // Verify APK signature matches expected certificate
        // In production, compare with known certificate fingerprint
        // For now, just verify it's a valid signed APK
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = android.content.pm.PackageManager.PackageInfoFlags.of(
                android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
            )
            val pkgInfo = context.packageManager.getPackageArchiveInfo(file.absolutePath, source)
            if (pkgInfo == null || pkgInfo.signingInfo?.signingCertificateHistory?.isEmpty() != false) {
                throw UpdateException("APK امضای معتبر ندارد")
            }
        }
    }

    companion object {
        const val PREF_LAST_UPDATE_CHECK = "last_update_check"
        const val PREF_AUTO_UPDATE_ENABLED = "auto_update_enabled"
    }
}

/**
 * Update states.
 */
sealed interface UpdateState {
    object Idle : UpdateState
    object Checking : UpdateState
    data class UpdateAvailable(
        val version: String,
        val releaseNotes: String,
        val apkUrl: String,
        val apkSize: Long,
        val checksumUrl: String?,
        val checksum: String?
    ) : UpdateState
    data class Downloaded(val file: File) : UpdateState
    object UpToDate : UpdateState
    data class Error(val message: String) : UpdateState
}

/**
 * Download progress.
 */
data class DownloadProgress(
    val downloaded: Long,
    val total: Long,
    val percentage: Float,
    val status: DownloadStatus
)

enum class DownloadStatus {
    Starting, Downloading, Verifying, ReadyToInstall, Error
}

/**
 * GitHub Release model.
 */
@Serializable
data class GitHubRelease(
    val tagName: String,
    val name: String,
    val body: String?,
    val assets: List<GitHubAsset>,
    val publishedAt: String
)

@Serializable
data class GitHubAsset(
    val name: String,
    val browserDownloadUrl: String,
    val size: Long
)

class UpdateException(message: String) : Exception(message)