package com.example.soundpayapplication.util

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BluetoothManager(private val context: Context) {

    private val TAG = "BluetoothManager"
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
        bluetoothManager?.adapter
    }

    private val _connectionState = MutableStateFlow(BluetoothConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var currentConnectedDevice: BluetoothDevice? = null

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    handleDeviceFound(intent)
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _isScanning.value = true
                    Log.d(TAG, "Bluetooth discovery started")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                    Log.d(TAG, "Bluetooth discovery finished")
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    handleDeviceConnected(intent)
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    handleDeviceDisconnected(intent)
                }
            }
        }
    }

    init {
        registerBluetoothReceiver()
    }

    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedAudioDevices(): List<BluetoothDeviceInfo> {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) {
            return emptyList()
        }

        return try {
            bluetoothAdapter?.bondedDevices
                ?.filter { isAudioDevice(it) }
                ?.map { device ->
                    BluetoothDeviceInfo(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        isPaired = true,
                        isConnected = isDeviceConnected(device)
                    )
                } ?: emptyList()
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission", e)
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) {
            Log.w(TAG, "Cannot start discovery: missing permissions or adapter")
            return
        }

        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter?.cancelDiscovery()
            }

            _discoveredDevices.value = emptyList()
            bluetoothAdapter?.startDiscovery()
            Log.d(TAG, "Started Bluetooth discovery")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission for discovery", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (!hasBluetoothPermissions() || bluetoothAdapter == null) {
            return
        }

        try {
            bluetoothAdapter?.cancelDiscovery()
            _isScanning.value = false
            Log.d(TAG, "Stopped Bluetooth discovery")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission for stopping discovery", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleDeviceFound(intent: Intent) {
        if (!hasBluetoothPermissions()) return

        try {
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            device?.let {
                if (isAudioDevice(it)) {
                    val deviceInfo = BluetoothDeviceInfo(
                        name = it.name ?: "Unknown Device",
                        address = it.address,
                        isPaired = it.bondState == BluetoothDevice.BOND_BONDED,
                        isConnected = false
                    )

                    val currentList = _discoveredDevices.value.toMutableList()
                    if (!currentList.any { existing -> existing.address == deviceInfo.address }) {
                        currentList.add(deviceInfo)
                        _discoveredDevices.value = currentList
                        Log.d(TAG, "Found audio device: ${deviceInfo.name}")
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleDeviceConnected(intent: Intent) {
        if (!hasBluetoothPermissions()) return

        try {
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            device?.let {
                if (isAudioDevice(it)) {
                    currentConnectedDevice = it
                    _connectionState.value = BluetoothConnectionState.CONNECTED
                    Log.d(TAG, "Audio device connected: ${it.name}")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleDeviceDisconnected(intent: Intent) {
        if (!hasBluetoothPermissions()) return

        try {
            val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
            }

            device?.let {
                if (currentConnectedDevice?.address == it.address) {
                    currentConnectedDevice = null
                    _connectionState.value = BluetoothConnectionState.DISCONNECTED
                    Log.d(TAG, "Audio device disconnected: ${it.name}")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing Bluetooth permission", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun isAudioDevice(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermissions()) return false

        return try {
            val audioClass = device.bluetoothClass?.deviceClass
            // Check if device is audio/headphones/speaker
            audioClass in listOf(
                0x0404, // Audio/Video - Wearable Headset
                0x0408, // Audio/Video - Handsfree
                0x0414, // Audio/Video - Loudspeaker
                0x0418, // Audio/Video - Portable Audio
                0x041C  // Audio/Video - Car Audio
            ) || device.bluetoothClass?.majorDeviceClass == 0x0400 // Audio/Video devices
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if device is audio", e)
            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        if (!hasBluetoothPermissions()) return false

        return try {
            device.bondState == BluetoothDevice.BOND_BONDED &&
            currentConnectedDevice?.address == device.address
        } catch (e: Exception) {
            false
        }
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
        Log.d(TAG, "Bluetooth receiver registered")
    }

    fun unregister() {
        try {
            stopDiscovery()
            context.unregisterReceiver(bluetoothReceiver)
            Log.d(TAG, "Bluetooth receiver unregistered")
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun getConnectedDeviceName(): String? {
        if (!hasBluetoothPermissions()) return null

        return try {
            currentConnectedDevice?.name
        } catch (e: SecurityException) {
            null
        }
    }

    fun openBluetoothSettings() {
        val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isPaired: Boolean,
    val isConnected: Boolean
)

enum class BluetoothConnectionState {
    CONNECTED,
    CONNECTING,
    DISCONNECTED,
    ERROR
}

