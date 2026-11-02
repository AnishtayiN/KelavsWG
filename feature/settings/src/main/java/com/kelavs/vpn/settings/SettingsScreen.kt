package com.kelavs.vpn.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
    layoutDirection: LayoutDirection = LayoutDirection.Rtl
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.material3.IconButton(onClick = onBackPressed) {
                Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = "تنظیمات",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(48.dp))
        }
        
        // Network Optimization
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("بهینه‌سازی شبکه", Icons.Default.Security)
                
                SettingRow(
                    title = "MTU",
                    subtitle = "حداکثر واحد انتقال (پیش‌فرض: خودکار)",
                    trailing = {
                        androidx.compose.material3.Text(
                            text = viewModel.mtuSetting,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { viewModel.showMtuDialog() }
                )
                
                SettingRow(
                    title = "DNS سفارشی",
                    subtitle = "استفاده از DNS اختصاصی به جای DNS کانفیگ",
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = viewModel.useCustomDns,
                            onCheckedChange = { viewModel.setCustomDns(it) }
                        )
                    }
                )
                
                SettingRow(
                    title = "حالت IPv6",
                    subtitle = "فعال/غیرفعال کردن پشتیبانی IPv6",
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = viewModel.enableIpv6,
                            onCheckedChange = { viewModel.setIpv6Enabled(it) }
                        )
                    }
                )
                
                SettingRow(
                    title = "Split Tunneling",
                    subtitle = "مسیردهی انتخاب‌شده برنامه‌ها (به‌زودی)",
                    trailing = {
                        androidx.compose.material3.Text("به‌زودی", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
            }
        }
        
        // Connection Behavior
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("رفتار اتصال", Icons.Default.Refresh)
                
                SettingRow(
                    title = "اتصال مجدد خودکار",
                    subtitle = "تلاش مجدد خودکار در صورت قطع اتصال",
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = viewModel.autoReconnect,
                            onCheckedChange = { viewModel.setAutoReconnect(it) }
                        )
                    }
                )
                
                SettingRow(
                    title = "تغییر شبکه",
                    subtitle = "بازیابی اتصال در هنگام تغییر وای‌فای/دیتا",
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = viewModel.handleNetworkChanges,
                            onCheckedChange = { viewModel.setHandleNetworkChanges(it) }
                        )
                    }
                )
                
                SettingRow(
                    title = "Persistent Keepalive",
                    subtitle = "حفظ تنظیمات Keepalive ارائه‌دهنده",
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = viewModel.respectKeepalive,
                            onCheckedChange = { viewModel.setRespectKeepalive(it) }
                        )
                    }
                )
            }
        }
        
        // Appearance
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("نمایش", Icons.Default.Palette)
                
                SettingRow(
                    title = "تم",
                    subtitle = "روشن/تیره/سیستمی",
                    trailing = {
                        androidx.compose.material3.Text(
                            text = viewModel.themeMode,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { viewModel.showThemeDialog() }
                )
                
                SettingRow(
                    title = "زبان",
                    subtitle = "فارسی/انگلیسی",
                    trailing = {
                        androidx.compose.material3.Text(
                            text = viewModel.language,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = { viewModel.showLanguageDialog() }
                )
            }
        }
        
        // Update
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("به‌روزرسانی", Icons.Default.CloudDownload)
                
                SettingRow(
                    title = "بررسی خودکار آپدیت",
                    subtitle = "بررسی نسخه جدید در GitHub Releases",
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = viewModel.autoUpdateCheck,
                            onCheckedChange = { viewModel.setAutoUpdateCheck(it) }
                        )
                    }
                )
                
                SettingRow(
                    title = "مخزن GitHub",
                    subtitle = "AnishtayiN/KelavsWG",
                    trailing = {
                        androidx.compose.material3.Text("v${viewModel.currentVersion}", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                )
                
                SettingRow(
                    title = "بررسی نسخه جدید",
                    subtitle = "بررسی دستی آپدیت",
                    trailing = {
                        Button(onClick = { viewModel.checkForUpdates() }) {
                            Text("بررسی", fontSize = 14.sp)
                        }
                    }
                )
            }
        }
        
        // Security
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("امنیت و حریم خصوصی", Icons.Default.Security)
                
                SettingRow(
                    title = "قفل بیومتریک",
                    subtitle = "محافظت از برنامه با اثر انگشت/چهره",
                    trailing = {
                        androidx.compose.material3.Switch(
                            checked = viewModel.biometricLock,
                            onCheckedChange = { viewModel.setBiometricLock(it) }
                        )
                    }
                )
                
                SettingRow(
                    title = "بدون لاگ",
                    subtitle = "برنامه هیچ اطلاعات حساسی ثبت نمی‌کند",
                    trailing = {
                        Icon(Icons.Default.Check, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    }
                )
                
                SettingRow(
                    title = "بدون تبلیغات و آنالیتیکس",
                    subtitle = "حریم خصوصی کامل",
                    trailing = {
                        Icon(Icons.Default.Check, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    }
                )
            }
        }
        
        // About
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader("درباره", Icons.Default.Info)
                
                SettingRow(
                    title = "نسخه برنامه",
                    subtitle = "v${viewModel.currentVersion} (${viewModel.buildNumber})",
                    trailing = {}
                )
                
                SettingRow(
                    title = "موتور WireGuard",
                    subtitle = "بر پایه WireGuard Android رسمی",
                    trailing = {}
                )
                
                SettingRow(
                    title = "مجوز",
                    subtitle = "Apache 2.0 / GPLv2",
                    trailing = {}
                )
                
                SettingRow(
                    title = "منبع باز",
                    subtitle = "GitHub: AnishtayiN/KelavsWG",
                    trailing = {}
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
        Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = title, fontSize = 16.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentWidth(Alignment.End),
            contentAlignment = Alignment.CenterEnd
        ) {
            trailing()
        }
    }
    
    if (onClick != null) {
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(0.dp))
        // Click handling would be added via Modifier.clickable
    }
}

// ViewModel placeholder
class SettingsViewModel {
    var mtuSetting = "خودکار" get() = field
    var useCustomDns = false
    var enableIpv6 = true
    var autoReconnect = true
    var handleNetworkChanges = true
    var respectKeepalive = true
    var themeMode = "سیستمی"
    var language = "فارسی"
    var autoUpdateCheck = true
    var biometricLock = false
    val currentVersion = "1.0.0"
    val buildNumber = 1
    
    fun showMtuDialog() { }
    fun showThemeDialog() { }
    fun showLanguageDialog() { }
    fun setCustomDns(enabled: Boolean) { useCustomDns = enabled }
    fun setIpv6Enabled(enabled: Boolean) { enableIpv6 = enabled }
    fun setAutoReconnect(enabled: Boolean) { autoReconnect = enabled }
    fun setHandleNetworkChanges(enabled: Boolean) { handleNetworkChanges = enabled }
    fun setRespectKeepalive(enabled: Boolean) { respectKeepalive = enabled }
    fun setAutoUpdateCheck(enabled: Boolean) { autoUpdateCheck = enabled }
    fun setBiometricLock(enabled: Boolean) { biometricLock = enabled }
    fun checkForUpdates() { }
}