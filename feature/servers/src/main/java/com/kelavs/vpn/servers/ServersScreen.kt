package com.kelavs.vpn.servers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ServersScreen(
    viewModel: ServersViewModel,
    onServerSelected: (ServerConfigEntity) -> Unit,
    modifier: Modifier = Modifier,
    layoutDirection: LayoutDirection = LayoutDirection.Rtl
) {
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val autoSelected by viewModel.autoSelectedServer.collectAsStateWithLifecycle()
    val manualSelected by viewModel.manualSelectedServer.collectAsStateWithLifecycle()
    
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
                text = "مدیریت سرورها",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
            
            // Auto selection toggle
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("انتخاب خودکار", fontSize = 14.sp)
                androidx.compose.material3.Switch(
                    checked = viewModel.isAutoSelectionEnabled,
                    onCheckedChange = { viewModel.setAutoSelectionEnabled(it) }
                )
            }
        }
        
        // Auto selection info
        if (viewModel.isAutoSelectionEnabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer)
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 12.dp))
                    Column {
                        Text("انتخاب خودکار فعال است", fontWeight = FontWeight.Bold, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("برنامه بر اساس Handshake موفق، تأخیر و پایداری بهترین سرور را انتخاب می‌کند", fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
        
        // Server list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(servers) { server ->
                ServerListItem(
                    server = server,
                    isAutoSelected = autoSelected?.publicKey == server.publicKey,
                    isManuallySelected = manualSelected?.publicKey == server.publicKey,
                    onClick = { onServerSelected(server) },
                    onToggle = { viewModel.toggleServerEnabled(server) }
                )
            }
        }
        
        if (servers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.WifiOff, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(64.dp))
                    Text("هیچ سروری یافت نشد", fontSize = 18.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("ابتدا اشتراک خود را وارد کنید", fontSize = 14.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun ServerListItem(
    server: ServerConfigEntity,
    isAutoSelected: Boolean,
    isManuallySelected: Boolean,
    onClick: () -> Unit,
    onToggle: () -> Unit
) {
    val isSelected = isAutoSelected || isManuallySelected
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (isSelected) 
                androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer 
            else 
                androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isAutoSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "انتخاب خودکار", tint = androidx.compose.material3.MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                } else if (isManuallySelected) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = "انتخاب دستی", tint = androidx.compose.material3.MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "انتخاب نشده", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
            
            // Server info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = server.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Enabled toggle
                    androidx.compose.material3.Switch(
                        checked = server.isEnabled,
                        onCheckedChange = { _ -> onToggle() },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = server.endpoint, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    if (server.avgLatencyMs > 0) {
                        Text(text = "${server.avgLatencyMs} ms", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                    }
                    
                    Text(
                        text = "${if (server.connectionCount > 0) (server.successCount * 100 / server.connectionCount) else 0}% موفقیت",
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            
            // Chevron
            Icon(
                imageVector = Icons.Default.SwapVert,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ViewModel placeholder
class ServersViewModel {
    val servers = kotlinx.coroutines.flow.MutableStateFlow<List<ServerConfigEntity>>(emptyList())
    val autoSelectedServer = kotlinx.coroutines.flow.MutableStateFlow<ServerConfigEntity?>(null)
    val manualSelectedServer = kotlinx.coroutines.flow.MutableStateFlow<ServerConfigEntity?>(null)
    var isAutoSelectionEnabled = true
        private set
    
    fun setAutoSelectionEnabled(enabled: Boolean) { isAutoSelectionEnabled = enabled }
    fun toggleServerEnabled(server: ServerConfigEntity) { /* Implementation */ }
}

// Placeholder data class
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