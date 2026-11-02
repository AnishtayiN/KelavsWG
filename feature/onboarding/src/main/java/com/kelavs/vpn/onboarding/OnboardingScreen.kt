package com.kelavs.vpn.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.singleWindowApplication
import com.kelavs.vpn.subscription.SubscriptionParser
import com.kelavs.vpn.subscription.SubscriptionParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSubscriptionLoaded: (SubscriptionParseResult) -> Unit,
    modifier: Modifier = Modifier,
    layoutDirection: LayoutDirection = LayoutDirection.Rtl
) {
    val context = LocalContext.current
    val scope = remember { CoroutineScope(Dispatchers.Main) }
    
    var subscriptionUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }
    
    val parser = remember { SubscriptionParser() }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo/Title area
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App icon placeholder
            Card(
                modifier = Modifier.size(96.dp),
                elevation = 4.dp
            ) {
                androidx.compose.foundation.Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 16.dp))
            
            Text(
                text = "به Kelavs VPN خوش آمدید",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 8.dp))
            
            Text(
                text = "برای شروع، لینک اشتراک خود را وارد کنید.",
                fontSize = 16.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        
        // Input field
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "لینک سابسکریپشن",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
                
                TextField(
                    value = subscriptionUrl,
                    onValueChange = { subscriptionUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com/subscription") },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        autoCorrect = false,
                        capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None
                    ),
                    singleLine = true,
                    isError = errorMessage != null,
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (showPassword) "مخفی کردن" else "نمایش",
                                tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (showPassword) androidx.compose.foundation.text.PasswordVisualTransformation() else androidx.compose.foundation.text.VisualTransformation.None
                )
                
                errorMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 12.sp,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        
        // Submit button
        Button(
            onClick = {
                if (subscriptionUrl.trim().isNotBlank()) {
                    loadSubscription(subscriptionUrl.trim())
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && subscriptionUrl.trim().isNotBlank(),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primary
            )
        ) {
            if (isLoading) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "دریافت و افزودن اشتراک",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
        
        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(start = 8.dp))
                    Text(
                        text = "راهنمای استفاده",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    infoRow("• لینک اشتراک را از ربات @Kelavs_bot دریافت کنید")
                    infoRow("• اشتراک می‌تواند شامل چندین کانفیگ WireGuard باشد")
                    infoRow("• برنامه به‌صورت خودکار بهترین سرور را انتخاب می‌کند")
                    infoRow("• نیازی به وارد کردن فایل کانفیگ به‌صورت دستی نیست")
                }
            }
        }
        
        // Telegram links
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "پشتیبانی و خرید اشتراک:",
                fontSize = 14.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TelegramButton(
                    text = "پشتیبانی @Matinkelavs",
                    url = "https://t.me/Matinkelavs"
                )
                TelegramButton(
                    text = "خرید اشتراک @Kelavs_bot",
                    url = "https://t.me/Kelavs_bot"
                )
            }
        }
    }
}

@Composable
private fun infoRow(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
    )
}

@Composable
private fun TelegramButton(text: String, url: String) {
    val context = LocalContext.current
    Button(
        onClick = {
            try {
                context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            } catch (e: Exception) {
                // Handle error silently
            }
        },
        modifier = Modifier.height(40.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

private fun OnboardingScreen.loadSubscription(url: String) {
    isLoading = true
    errorMessage = null
    
    scope.launch(Dispatchers.IO) {
        val result = parser.parseSubscription(url)
        
        withContext(Dispatchers.Main) {
            isLoading = false
            when (result) {
                is SubscriptionParseResult.Success -> {
                    onSubscriptionLoaded(result.value)
                }
                is SubscriptionParseResult.Failure -> {
                    errorMessage = result.exception.message ?: "خطای نامشخص در پردازش اشتراک"
                }
            }
        }
    }
}

// Sealed interface for result handling
sealed interface SubscriptionParseResult {
    data class Success(val value: com.kelavs.vpn.subscription.SubscriptionParseResult) : SubscriptionParseResult
    data class Failure(val exception: Exception) : SubscriptionParseResult
}