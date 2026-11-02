package com.kelavs.vpn.subscription_ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filed.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel,
    modifier: Modifier = Modifier,
    layoutDirection: LayoutDirection = LayoutDirection.Rtl
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    val subscription by viewModel.subscription.collectAsStateWithLifecycle()
    var showUrl by remember { mutableStateOf(false) }
    
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
            Text(
                text = "اشتراک",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Subscription status card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = when {
                    subscription?.isValid == true -> androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                    subscription?.isValid == false -> androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                    else -> androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            subscription?.isValid == true -> "اشتراک فعال"
                            subscription?.isValid == false -> "اشتراک نامعتبر"
                            else -> "اشتراکی تنظیم نشده"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            subscription?.isValid == true -> androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                            subscription?.isValid == false -> androidx.compose.material3.MaterialTheme.colorScheme.onErrorContainer
                            else -> androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    if (subscription != null) {
                        IconButton(onClick = { showUrl = !showUrl }) {
                            Icon(
                                imageVector = if (showUrl) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showUrl) "مخفی کردن لینک" else "نمایش لینک",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                subscription?.let { sub ->
                    InfoRow("آخرین به‌روزرسانی موفق", formatDate(sub.lastUpdated))
                    InfoRow("آخرین تلاش برای به‌روزرسانی", formatDate(sub.lastUpdateAttempt))
                    InfoRow("تعداد کانفیگ‌های معتبر", sub.configCount.toString())
                    
                    if (showUrl) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Text(
                                text = maskUrl(sub.sourceUrl),
                                modifier = Modifier.padding(12.dp),
                                fontSize = 12.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    if (sub.errorMessage != null) {
                        Text(
                            text = "خطا: ${sub.errorMessage}",
                            fontSize = 12.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        // Action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.refreshSubscription() },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = subscription != null && !viewModel.isRefreshing,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (viewModel.isRefreshing) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    }
                    Text(
                        text = if (viewModel.isRefreshing) "در حال به‌روزرسانی..." else "به‌روزرسانی اشتراک",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            if (subscription != null) {
                Button(
                    onClick = { viewModel.deleteSubscription() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp), tint = androidx.compose.material3.MaterialTheme.colorScheme.error)
                        Text(
                            text = "حذف اشتراک",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        
        // Auto-update settings
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("تنظیمات به‌روزرسانی خودکار", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("به‌روزرسانی دوره‌ای", fontSize = 14.sp)
                        Text("اشتراک به‌صورت خودکار در پس‌زمینه به‌روزرسانی می‌شود", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(
                        checked = viewModel.isAutoUpdateEnabled,
                        onCheckedChange = { viewModel.setAutoUpdateEnabled(it) }
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("حفظ کانفیگ‌ها در خطا", fontSize = 14.sp)
                        Text("در صورت خطای موقت، کانفیگ‌های قبلی حفظ می‌شوند", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    androidx.compose.material3.Switch(
                        checked = viewModel.isKeepConfigsOnError,
                        onCheckedChange = { viewModel.setKeepConfigsOnError(it) }
                    )
                }
            }
        }
        
        // Telegram links
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("پشتیبانی و خرید اشتراک:", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelegramButton(text = "پشتیبانی @Matinkelavs", url = "https://t.me/Matinkelavs")
                TelegramButton(text = "خرید اشتراک @Kelavs_bot", url = "https://t.me/Kelavs_bot")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TelegramButton(text: String, url: String) {
    val context = LocalContext.current
    Button(
        onClick = {
            try {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            } catch (e: Exception) { }
        },
        modifier = Modifier.height(40.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(text = text, fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer)
    }
}

private fun maskUrl(url: String): String {
    try {
        val uri = android.net.Uri.parse(url)
        val host = uri.host ?: return url
        val path = uri.path ?: ""
        return "https://$host${if (path.length > 20) path.substring(0, 20) + "..." else path}"
    } catch (e: Exception) {
        return url
    }
}

private fun formatDate(timestamp: Long): String {
    return java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
}

// ViewModel placeholder
class SubscriptionViewModel {
    val subscription = kotlinx.coroutines.flow.MutableStateFlow<SubscriptionEntity?>(null)
    var isRefreshing = false
    var isAutoUpdateEnabled = true
    var isKeepConfigsOnError = true
    
    fun refreshSubscription() { /* Implementation */ }
    fun deleteSubscription() { /* Implementation */ }
    fun setAutoUpdateEnabled(enabled: Boolean) { isAutoUpdateEnabled = enabled }
    fun setKeepConfigsOnError(enabled: Boolean) { isKeepConfigsOnError = enabled }
}

// Placeholder
data class SubscriptionEntity(
    val sourceUrl: String,
    val lastUpdated: Long,
    val lastUpdateAttempt: Long,
    val configCount: Int,
    val isValid: Boolean,
    val errorMessage: String? = null
)