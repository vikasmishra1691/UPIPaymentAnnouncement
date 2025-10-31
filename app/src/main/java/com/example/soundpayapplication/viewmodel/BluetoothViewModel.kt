package com.example.soundpayapplication.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.soundpayapplication.util.BluetoothConnectionState
import com.example.soundpayapplication.util.BluetoothDeviceInfo
import com.example.soundpayapplication.util.BluetoothManager
import com.example.soundpayapplication.util.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BluetoothViewModel"
    private val context: Context = application.applicationContext
    private val preferenceManager = PreferenceManager(context)
    private val bluetoothManager = BluetoothManager(context)

    private val _isBluetoothEnabled = MutableStateFlow(preferenceManager.isBluetoothEnabled())
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceInfo>> = _pairedDevices.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(BluetoothConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceInfo?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceInfo?> = _selectedDevice.asStateFlow()

    private val _hasBluetoothPermissions = MutableStateFlow(false)
    val hasBluetoothPermissions: StateFlow<Boolean> = _hasBluetoothPermissions.asStateFlow()

    private val _isBluetoothSupported = MutableStateFlow(true)
    val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

    private val _isSystemBluetoothEnabled = MutableStateFlow(false)
    val isSystemBluetoothEnabled: StateFlow<Boolean> = _isSystemBluetoothEnabled.asStateFlow()

    init {
        checkBluetoothSupport()
        checkBluetoothPermissions()
        checkSystemBluetoothEnabled()
        observeBluetoothState()
        loadSavedDevice()
        loadPairedDevices()
    }

    private fun checkBluetoothSupport() {
        _isBluetoothSupported.value = bluetoothManager.isBluetoothSupported()
        if (!_isBluetoothSupported.value) {
            Log.w(TAG, "Bluetooth not supported on this device")
        }
    }

    private fun checkBluetoothPermissions() {
        _hasBluetoothPermissions.value = bluetoothManager.hasBluetoothPermissions()
    }

    private fun checkSystemBluetoothEnabled() {
        _isSystemBluetoothEnabled.value = bluetoothManager.isBluetoothEnabled()
    }

    fun updatePermissionStatus() {
        checkBluetoothPermissions()
        if (_hasBluetoothPermissions.value) {
            loadPairedDevices()
        }
    }

    fun updateSystemBluetoothStatus() {
        checkSystemBluetoothEnabled()
        if (_isSystemBluetoothEnabled.value) {
            loadPairedDevices()
        }
    }

    private fun observeBluetoothState() {
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { state ->
                _connectionState.value = state
                Log.d(TAG, "Connection state changed: $state")
            }
        }

        viewModelScope.launch {
            bluetoothManager.discoveredDevices.collect { devices ->
                _discoveredDevices.value = devices
                Log.d(TAG, "Discovered devices updated: ${devices.size} devices")
            }
        }

        viewModelScope.launch {
            bluetoothManager.isScanning.collect { scanning ->
                _isScanning.value = scanning
            }
        }
    }

    private fun loadSavedDevice() {
        val savedAddress = preferenceManager.getBluetoothDeviceAddress()
        val savedName = preferenceManager.getBluetoothDeviceName()

        if (savedAddress != null && savedName != null) {
            _selectedDevice.value = BluetoothDeviceInfo(
                name = savedName,
                address = savedAddress,
                isPaired = true,
                isConnected = false
            )
            Log.d(TAG, "Loaded saved device: $savedName")
        }
    }

    fun loadPairedDevices() {
        viewModelScope.launch {
            try {
                val devices = bluetoothManager.getPairedAudioDevices()
                _pairedDevices.value = devices
                Log.d(TAG, "Loaded ${devices.size} paired audio devices")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading paired devices", e)
            }
        }
    }

    fun startDiscovery() {
        if (!_hasBluetoothPermissions.value) {
            Log.w(TAG, "Cannot start discovery: missing permissions")
            return
        }

        if (!_isSystemBluetoothEnabled.value) {
            Log.w(TAG, "Cannot start discovery: Bluetooth is disabled")
            return
        }

        viewModelScope.launch {
            try {
                bluetoothManager.startDiscovery()
                Log.d(TAG, "Started device discovery")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting discovery", e)
            }
        }
    }

    fun stopDiscovery() {
        viewModelScope.launch {
            try {
                bluetoothManager.stopDiscovery()
                Log.d(TAG, "Stopped device discovery")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery", e)
            }
        }
    }

    fun setBluetoothEnabled(enabled: Boolean) {
        _isBluetoothEnabled.value = enabled
        preferenceManager.setBluetoothEnabled(enabled)
        Log.d(TAG, "Bluetooth announcements ${if (enabled) "enabled" else "disabled"}")
    }

    fun selectDevice(device: BluetoothDeviceInfo) {
        _selectedDevice.value = device
        preferenceManager.setBluetoothDeviceAddress(device.address)
        preferenceManager.setBluetoothDeviceName(device.name)
        Log.d(TAG, "Selected device: ${device.name}")
    }

    fun clearSelectedDevice() {
        _selectedDevice.value = null
        preferenceManager.setBluetoothDeviceAddress(null)
        preferenceManager.setBluetoothDeviceName(null)
        Log.d(TAG, "Cleared selected device")
    }

    fun openBluetoothSettings() {
        bluetoothManager.openBluetoothSettings()
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.unregister()
        Log.d(TAG, "ViewModel cleared, Bluetooth manager unregistered")
    }
}

