package com.kelavs.vpn.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToServers: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    layoutDirection: LayoutDirection = LayoutDirection.Rtl
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with app name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Kelavs VPN",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = { onNavigateToSubscription() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "به‌روزرسانی اشتراک")
                }
                IconButton(onClick = { onNavigateToSettings() }) {
                    Icon(Icons.Default.Settings, contentDescription = "تنظیمات")
                }
            }
        }
        
        // Connection Status Card
        ConnectionStatusCard(
            state = connectionState,
            onToggle = { viewModel.toggleConnection() },
            onServerClick = { onNavigateToServers() }
        )
        
        // Server Info Card
        if (selectedServer != null) {
            ServerInfoCard(
                server = selectedServer,
                onChangeServer = { onNavigateToServers() }
            )
        }
        
        // Statistics Cards
        StatisticsSection(stats = stats)
        
        // Troubleshooting button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            onClick = { /* Navigate to troubleshooting */ }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 12.dp))
                    Text(
                        text = "عیب‌یابی و لاگ اتصال",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = null,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ConnectionStatusCard(
    state: ConnectionState,
    onToggle: () -> Unit,
    onServerClick: () -> Unit
) {
    val (color, icon, text) = when (state) {
        ConnectionState.Connected -> 
            androidx.compose.material3.MaterialTheme.colorScheme.primary to Icons.Default.Wifi to "متصل است"
        ConnectionState.Connecting -> 
            androidx.compose.material3.MaterialTheme.colorScheme.secondary to Icons.Default.Refresh to "در حال اتصال..."
        ConnectionState.Reconnecting -> 
            androidx.compose.material3.MaterialTheme.colorScheme.tertiary to Icons.Default.SwapVert to "در حال اتصال مجدد..."
        ConnectionState.Disconnected -> 
            androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant to Icons.Default.WifiOff to "متصل نیست"
        ConnectionState.Error(msg) -> 
            androidx.compose.material3.MaterialTheme.colorScheme.error to Icons.Default.WifiOff to "خطا: $msg"
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status icon
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(48.dp)
                )
                
                // Pulse animation for connecting states
                if (state == ConnectionState.Connecting || state == ConnectionState.Reconnecting) {
                    androidx.compose.animation.core.AnimatedVisibility(
                        visible = true,
                        enter = androidx.compose.animation.core.fadeIn(),
                        exit = androidx.compose.animation.core.fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .graphicsLayer { alpha = 0.3f }
                                .background(color, RoundedCornerShape(40.dp))
                        )
                    }
                }
            }
            
            // Status text
            Text(
                text = text,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            
            // Toggle button
            Button(
                onClick = onToggle,
                modifier = Modifier
                    .fillMaxWidth()
                    .width(200.dp)
                    .height(56.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = when {
                        state == ConnectionState.Connected -> androidx.compose.material3.MaterialTheme.colorScheme.error
                        state.isConnecting -> androidx.compose.material3.MaterialTheme.colorScheme.secondary
                        else -> androidx.compose.material3.MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                Text(
                    text = when {
                        state == ConnectionState.Connected -> "قطع اتصال"
                        state.isConnecting -> "لغو"
                        else -> "اتصال"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                )
            }
            
            // Change server hint
            if (state == ConnectionState.Connected) {
                Text(
                    text = "برای تغییر سرور، روی کارت سرور بزنید",
                    fontSize = 12.sp,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ServerInfoCard(
    server: ServerConfigEntity,
    onChangeServer: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        onClick = onChangeServer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = server.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = server.endpoint,
                        fontSize = 14.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Server status indicator
                Box(
                    modifier = Modifier.size(12.dp)
                        .background(
                            if (server.isEnabled) androidx.compose.material3.MaterialTheme.colorScheme.primary 
                            else androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StatItem(
                    label = "تأخیر",
                    value = if (server.avgLatencyMs > 0) "${server.avgLatencyMs} ms" else "—",
                    icon = Icons.Default.Wifi
                )
                StatItem(
                    label = "اتصالات",
                    value = server.connectionCount.toString(),
                    icon = Icons.Default.SwapVert
                )
                StatItem(
                    label = "موفقیت",
                    value = "${if (server.connectionCount > 0) (server.successCount * 100 / server.connectionCount) else 0}%",
                    icon = Icons.Default.Check
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun StatisticsSection(stats: ConnectionStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "دریافتی",
            value = formatBytes(stats.bytesReceived),
            icon = Icons.Default.CloudDownload,
            color = androidx.compose.material3.MaterialTheme.colorScheme.primary
        )
        StatCard(
            title = "ارسالی",
            value = formatBytes(stats.bytesSent),
            icon = Icons.Default.CloudUpload,
            color = androidx.compose.material3.MaterialTheme.colorScheme.secondary
        )
        StatCard(
            title = "مدت اتصال",
            value = formatDuration(stats.durationMs),
            icon = Icons.Default.Timer,
            color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary
        )
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Card(
        modifier = Modifier
            .weight(1f)
            .height(100.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = title, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes} B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

private fun formatDuration(ms: Long): String {
    val seconds = ms / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    return when {
        hours > 0 -> "${hours}س ${minutes % 60}د"
        minutes > 0 -> "${minutes}د ${seconds % 60}ث"
        else -> "${seconds}ث"
    }
}

// ViewModel and State classes would be defined here
sealed interface ConnectionState {
    data class Connected(val serverName: String) : ConnectionState
    object Connecting : ConnectionState
    object Reconnecting : ConnectionState
    object Disconnected : ConnectionState
    data class Error(val message: String) : ConnectionState
    
    val isConnecting: Boolean
        get() = this is Connecting || this is Reconnecting
}

data class ConnectionStats(
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val durationMs: Long = 0
)

// Placeholder for ServerConfigEntity reference
data class ServerConfigEntity(
    val publicKey: String,
    val name: String,
    val endpoint: String,
    val address: String,
    val dns: String?,
    val mtu: Int?,
    val persistentKeepalive: Int?,
    val allowedIPs: String,
    val presharedKey: String?,
    val sourceSubscriptionUrl: String,
    val addedAt: Long,
    val lastConnected: Long,
    val connectionCount: Int,
    val successCount: Int,
    val failureCount: Int,
    val avgLatencyMs: Long,
    val lastHandshakeAt: Long,
    val isEnabled: Boolean,
    val isAutoSelected: Boolean
)

// ViewModel placeholder
class DashboardViewModel {
    val connectionState = kotlinx.coroutines.flow.MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val selectedServer = kotlinx.coroutines.flow.MutableStateFlow<ServerConfigEntity?>(null)
    val stats = kotlinx.coroutines.flow.MutableStateFlow<ConnectionStats>(ConnectionStats())
    
    fun toggleConnection() { /* Implementation */ }
}