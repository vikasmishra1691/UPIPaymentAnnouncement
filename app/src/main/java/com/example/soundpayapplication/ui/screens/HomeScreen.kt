package com.example.soundpayapplication.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soundpayapplication.ui.components.BluetoothSpeakerCard
import com.example.soundpayapplication.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("DEPRECATION")
fun HomeScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val context = LocalContext.current
    val todayTotal by viewModel.todayTotal.collectAsState()
    val todayCount by viewModel.todayCount.collectAsState()
    val weekTotal by viewModel.weekTotal.collectAsState()
    val monthTotal by viewModel.monthTotal.collectAsState()

    val isListenerEnabled = remember {
        mutableStateOf(isNotificationListenerEnabled(context))
    }

    LaunchedEffect(Unit) {
        viewModel.loadStatistics()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SoundPay", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isListenerEnabled.value)
                        Color(0xFF4CAF50) else Color(0xFFF44336)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isListenerEnabled.value)
                                Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                        Column {
                            Text(
                                text = if (isListenerEnabled.value)
                                    "Service Active" else "Service Inactive",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isListenerEnabled.value)
                                    "Monitoring payments" else "Enable notification access",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }

                    if (!isListenerEnabled.value) {
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFFF44336)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Enable", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Horizontal Statistics Cards (Today, Week, Month)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompactStatCard(
                    title = "Today",
                    amount = todayTotal,
                    count = todayCount,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF2196F3)
                )
                CompactStatCard(
                    title = "This Week",
                    amount = weekTotal,
                    count = null,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFF9C27B0)
                )
                CompactStatCard(
                    title = "This Month",
                    amount = monthTotal,
                    count = null,
                    modifier = Modifier.weight(1f),
                    color = Color(0xFFFF9800)
                )
            }

            // Transaction History Button
            Button(
                onClick = onNavigateToHistory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transaction History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Bluetooth Speaker Card
            Text(
                text = "Audio Output",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            BluetoothSpeakerCard()

            // Quick Actions
            Text(
                text = "Quick Actions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            ActionCard(
                title = "Settings",
                icon = Icons.Default.Settings,
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            )

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Automatically announces UPI payments from PhonePe, Google Pay, Paytm, BHIM, and more.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun CompactStatCard(
    title: String,
    amount: Double,
    count: Int?,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = formatCurrency(amount),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            count?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$it txn",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    amount: Double,
    count: Int?,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(amount),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            count?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$it payments",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun isNotificationListenerEnabled(context: android.content.Context): Boolean {
    val packageName = context.packageName
    val flat = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return flat != null && flat.contains(packageName)
}

private fun formatCurrency(amount: Double): String {
    val locale = Locale.Builder().setLanguage("en").setRegion("IN").build()
    val format = NumberFormat.getCurrencyInstance(locale)
    return format.format(amount)
}

