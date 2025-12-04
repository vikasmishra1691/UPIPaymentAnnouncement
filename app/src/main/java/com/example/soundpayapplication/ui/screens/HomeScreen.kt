package com.example.soundpayapplication.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import android.util.Log
import com.example.soundpayapplication.viewmodel.PaymentViewModel
import com.example.soundpayapplication.util.PreferenceManager
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

    // Log state changes for debugging
    LaunchedEffect(todayTotal, todayCount, weekTotal, monthTotal) {
        Log.d("HomeScreen", "UI State Updated:")
        Log.d("HomeScreen", "  Today Total: ₹$todayTotal (Count: $todayCount)")
        Log.d("HomeScreen", "  Week Total: ₹$weekTotal")
        Log.d("HomeScreen", "  Month Total: ₹$monthTotal")
    }

    // Load statistics on initial composition
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "Initial load - fetching statistics")
        viewModel.loadStatistics()
    }

    // Refresh statistics every time the screen resumes
    LifecycleResumeEffect(Unit) {
        Log.d("HomeScreen", "Screen resumed - refreshing statistics")
        viewModel.loadStatistics()

        onPauseOrDispose {
            Log.d("HomeScreen", "Screen paused")
        }
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
                .verticalScroll(rememberScrollState())
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

            // Today's Earning Card (Larger)
            EarningsCard(
                title = "Today's Earning",
                amount = todayTotal,
                count = todayCount,
                icon = Icons.Default.TrendingUp,
                isLarge = true
            )

            // This Week's Earning Card
            EarningsCard(
                title = "This Week's Earning",
                amount = weekTotal,
                count = null,
                icon = Icons.Default.CalendarMonth,
                isLarge = false
            )

            // This Month's Earning Card
            EarningsCard(
                title = "This Month's Earning",
                amount = monthTotal,
                count = null,
                icon = Icons.Default.CalendarToday,
                isLarge = false
            )

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

            // Divider before Settings Section
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Settings Section Header
            Text(
                text = "Settings & Preferences",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Settings Content
            SettingsContent()
        }
    }
}

@Composable
fun SettingsContent() {
    val context = LocalContext.current
    val preferenceManager = remember { PreferenceManager(context) }

    var announcementEnabled by remember {
        mutableStateOf(preferenceManager.isAnnouncementEnabled())
    }
    var volumeBoostEnabled by remember {
        mutableStateOf(preferenceManager.isVolumeBoostEnabled())
    }
    var autoStartEnabled by remember {
        mutableStateOf(preferenceManager.isAutoStartEnabled())
    }
    var selectedLanguage by remember {
        mutableStateOf(preferenceManager.getLanguage())
    }

    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Announcement Settings
        SettingsSection(title = "Announcement") {
            SettingSwitchItem(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Enable Announcements",
                description = "Announce payments via speaker",
                checked = announcementEnabled,
                onCheckedChange = {
                    announcementEnabled = it
                    preferenceManager.setAnnouncementEnabled(it)
                }
            )

            HorizontalDivider()

            SettingClickItem(
                icon = Icons.Default.Language,
                title = "Language",
                description = selectedLanguage.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                },
                onClick = { showLanguageDialog = true }
            )
        }

        // Audio Settings
        SettingsSection(title = "Audio") {
            SettingSwitchItem(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = "Volume Boost",
                description = "Automatically increase volume for announcements",
                checked = volumeBoostEnabled,
                onCheckedChange = {
                    volumeBoostEnabled = it
                    preferenceManager.setVolumeBoostEnabled(it)
                }
            )
        }

        // System Settings
        SettingsSection(title = "System") {
            SettingSwitchItem(
                icon = Icons.Default.PowerSettingsNew,
                title = "Auto Start on Boot",
                description = "Start service when device boots",
                checked = autoStartEnabled,
                onCheckedChange = {
                    autoStartEnabled = it
                    preferenceManager.setAutoStartEnabled(it)
                }
            )
        }

        // About Section
        SettingsSection(title = "About") {
            SettingInfoItem(
                icon = Icons.Default.Info,
                title = "Version",
                description = "1.0.0"
            )

            HorizontalDivider()

            SettingInfoItem(
                icon = Icons.Default.Security,
                title = "Privacy",
                description = "Your payment data is stored locally and never shared"
            )
        }

        // Supported Apps
        SettingsSection(title = "Supported UPI Apps") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val apps = listOf(
                    "PhonePe", "Google Pay", "Paytm", "BHIM",
                    "Amazon Pay", "BharatPe", "Freecharge", "MobiKwik",
                    "WhatsApp", "CRED", "Airtel Payments", "iMobile Pay",
                    "Axis Mobile", "YONO SBI", "HDFC Bank", "Federal Bank", "PNB One"
                )
                apps.forEach { app ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = app,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    RadioButtonItem(
                        text = "English",
                        selected = selectedLanguage == "english",
                        onClick = {
                            selectedLanguage = "english"
                            preferenceManager.setLanguage("english")
                            showLanguageDialog = false
                        }
                    )
                    RadioButtonItem(
                        text = "Hindi",
                        selected = selectedLanguage == "hindi",
                        onClick = {
                            selectedLanguage = "hindi"
                            preferenceManager.setLanguage("hindi")
                            showLanguageDialog = false
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EarningsCard(
    title: String,
    amount: Double,
    count: Int?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isLarge: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isLarge) 20.dp else 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Icon and Title
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(if (isLarge) 48.dp else 40.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = if (isLarge) 18.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    count?.let {
                        Text(
                            text = "$it transaction${if (it != 1) "s" else ""}",
                            fontSize = if (isLarge) 13.sp else 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Right side - Amount
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = formatCurrency(amount),
                    fontSize = if (isLarge) 28.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
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


