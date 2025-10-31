package com.example.soundpayapplication.ui.components

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.soundpayapplication.util.BluetoothDeviceInfo
import com.example.soundpayapplication.viewmodel.BluetoothViewModel

@Composable
fun BluetoothSpeakerCard(
    bluetoothViewModel: BluetoothViewModel = viewModel()
) {
    val isBluetoothEnabled by bluetoothViewModel.isBluetoothEnabled.collectAsState()
    val selectedDevice by bluetoothViewModel.selectedDevice.collectAsState()
    val hasPermissions by bluetoothViewModel.hasBluetoothPermissions.collectAsState()
    val isSystemBluetoothEnabled by bluetoothViewModel.isSystemBluetoothEnabled.collectAsState()
    val isBluetoothSupported by bluetoothViewModel.isBluetoothSupported.collectAsState()

    var showDeviceSelectionDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }

    // Permission launcher for Android 12+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            bluetoothViewModel.updatePermissionStatus()
            bluetoothViewModel.loadPairedDevices()
        } else {
            showPermissionRationale = true
        }
    }

    // Bluetooth enable launcher
    val bluetoothEnableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        bluetoothViewModel.updateSystemBluetoothStatus()
    }

    fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
                )
            )
        }
    }

    fun requestBluetoothEnable() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        bluetoothEnableLauncher.launch(enableBtIntent)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (isBluetoothEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Bluetooth Speaker",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = isBluetoothEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            when {
                                !isBluetoothSupported -> {
                                    // Show not supported message
                                }
                                !hasPermissions -> {
                                    requestBluetoothPermissions()
                                }
                                !isSystemBluetoothEnabled -> {
                                    requestBluetoothEnable()
                                }
                                else -> {
                                    bluetoothViewModel.setBluetoothEnabled(true)
                                }
                            }
                        } else {
                            bluetoothViewModel.setBluetoothEnabled(false)
                        }
                    },
                    enabled = isBluetoothSupported
                )
            }

            if (!isBluetoothSupported) {
                Text(
                    text = "Bluetooth not supported on this device",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (isBluetoothEnabled) {
                HorizontalDivider()

                // Status Section
                when {
                    !hasPermissions -> {
                        InfoRow(
                            icon = Icons.Default.Warning,
                            text = "Bluetooth permission required",
                            iconTint = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { requestBluetoothPermissions() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Permission")
                        }
                    }
                    !isSystemBluetoothEnabled -> {
                        InfoRow(
                            icon = Icons.Default.BluetoothDisabled,
                            text = "Bluetooth is turned off",
                            iconTint = MaterialTheme.colorScheme.error
                        )
                        Button(
                            onClick = { requestBluetoothEnable() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enable Bluetooth")
                        }
                    }
                    selectedDevice != null -> {
                        InfoRow(
                            icon = Icons.Default.BluetoothConnected,
                            text = selectedDevice!!.name,
                            iconTint = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showDeviceSelectionDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Change")
                            }
                            OutlinedButton(
                                onClick = { bluetoothViewModel.clearSelectedDevice() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Disconnect")
                            }
                        }
                    }
                    else -> {
                        InfoRow(
                            icon = Icons.Default.BluetoothDisabled,
                            text = "No device selected",
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showDeviceSelectionDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Device")
                        }
                    }
                }

                // Help text
                Text(
                    text = "Announcements will play through the selected Bluetooth speaker",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else {
                Text(
                    text = "Enable to play announcements through Bluetooth speaker",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    // Device Selection Dialog
    if (showDeviceSelectionDialog) {
        BluetoothDeviceSelectionDialog(
            onDismiss = { showDeviceSelectionDialog = false },
            onDeviceSelected = { device ->
                bluetoothViewModel.selectDevice(device)
                showDeviceSelectionDialog = false
            },
            bluetoothViewModel = bluetoothViewModel
        )
    }

    // Permission Rationale Dialog
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Bluetooth Permission Required") },
            text = {
                Text("SoundPay needs Bluetooth permission to connect to your Bluetooth speaker for payment announcements. Please grant the permission in Settings.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionRationale = false
                    bluetoothViewModel.openBluetoothSettings()
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconTint: androidx.compose.ui.graphics.Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceSelectionDialog(
    onDismiss: () -> Unit,
    onDeviceSelected: (BluetoothDeviceInfo) -> Unit,
    bluetoothViewModel: BluetoothViewModel
) {
    val pairedDevices by bluetoothViewModel.pairedDevices.collectAsState()
    val discoveredDevices by bluetoothViewModel.discoveredDevices.collectAsState()
    val isScanning by bluetoothViewModel.isScanning.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        bluetoothViewModel.loadPairedDevices()
    }

    AlertDialog(
        onDismissRequest = {
            bluetoothViewModel.stopDiscovery()
            onDismiss()
        }
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Select Bluetooth Device",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            bluetoothViewModel.stopDiscovery()
                        },
                        text = { Text("Paired") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            bluetoothViewModel.startDiscovery()
                        },
                        text = { Text("Available") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Device List
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            if (pairedDevices.isEmpty()) {
                                EmptyDeviceList("No paired Bluetooth speakers found")
                            } else {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(pairedDevices) { device ->
                                        DeviceListItem(
                                            device = device,
                                            onClick = { onDeviceSelected(device) }
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            when {
                                isScanning -> {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            CircularProgressIndicator()
                                            Text("Scanning for devices...")
                                        }
                                    }
                                }
                                discoveredDevices.isEmpty() -> {
                                    EmptyDeviceList("No devices found")
                                }
                                else -> {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(discoveredDevices) { device ->
                                            DeviceListItem(
                                                device = device,
                                                onClick = { onDeviceSelected(device) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedTab == 1) {
                        OutlinedButton(
                            onClick = {
                                if (isScanning) {
                                    bluetoothViewModel.stopDiscovery()
                                } else {
                                    bluetoothViewModel.startDiscovery()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (isScanning) Icons.Default.Stop else Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isScanning) "Stop" else "Scan")
                        }
                    }
                    TextButton(
                        onClick = {
                            bluetoothViewModel.stopDiscovery()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: BluetoothDeviceInfo,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
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
                    imageVector = if (device.isPaired) Icons.Default.Bluetooth
                                 else Icons.Default.BluetoothConnected,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = device.name,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (device.isPaired) "Paired" else "Available",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyDeviceList(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BluetoothDisabled,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

