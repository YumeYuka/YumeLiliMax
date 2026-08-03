package moe.yumeyuka.lilimax.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface BluetoothProfileEvent {
    val device: BluetoothDevice

    data class CurrentAudioDevice(override val device: BluetoothDevice) : BluetoothProfileEvent

    data class AclConnected(override val device: BluetoothDevice) : BluetoothProfileEvent

    data class AclDisconnected(override val device: BluetoothDevice) : BluetoothProfileEvent

    data class A2dpConnected(override val device: BluetoothDevice) : BluetoothProfileEvent

    data class A2dpDisconnected(override val device: BluetoothDevice) : BluetoothProfileEvent
}

@SuppressLint("MissingPermission")
class BluetoothProfileCoordinator(context: Context) {
    private val ctx = context.applicationContext
    private val adapter =
        (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)
            ?.adapter

    private val _events =
        MutableSharedFlow<BluetoothProfileEvent>(
            extraBufferCapacity = 32,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )
    val events: SharedFlow<BluetoothProfileEvent> = _events.asSharedFlow()

    private var closed = false

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val device = intent.bluetoothDevice() ?: return
                val event =
                    when (intent.action) {
                        BluetoothDevice.ACTION_ACL_CONNECTED ->
                            BluetoothProfileEvent.AclConnected(device)
                        BluetoothDevice.ACTION_ACL_DISCONNECTED ->
                            BluetoothProfileEvent.AclDisconnected(device)
                        BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED ->
                            when (
                                intent.getIntExtra(
                                    BluetoothProfile.EXTRA_STATE,
                                    BluetoothProfile.STATE_DISCONNECTED,
                                )
                            ) {
                                BluetoothProfile.STATE_CONNECTED ->
                                    BluetoothProfileEvent.A2dpConnected(device)
                                BluetoothProfile.STATE_DISCONNECTED ->
                                    BluetoothProfileEvent.A2dpDisconnected(device)
                                else -> null
                            }
                        else -> null
                    }
                event?.let(_events::tryEmit)
            }
        }

    init {
        val filter =
            IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            ctx.registerReceiver(receiver, filter)
        }
    }

    fun findConnectedAudioDevices() {
        val currentAdapter = adapter ?: return
        queryConnectedProfile(currentAdapter, BluetoothProfile.A2DP) { devices ->
            if (devices.isNotEmpty()) {
                devices.forEach { _events.tryEmit(BluetoothProfileEvent.CurrentAudioDevice(it)) }
            } else {
                queryConnectedProfile(currentAdapter, BluetoothProfile.HEADSET) { headsetDevices ->
                    headsetDevices.forEach {
                        _events.tryEmit(BluetoothProfileEvent.CurrentAudioDevice(it))
                    }
                }
            }
        }
    }

    private fun queryConnectedProfile(
        currentAdapter: android.bluetooth.BluetoothAdapter,
        profile: Int,
        onResult: (List<BluetoothDevice>) -> Unit,
    ) {
        currentAdapter.getProfileProxy(
            ctx,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(connectedProfile: Int, proxy: BluetoothProfile) {
                    val devices =
                        try {
                            proxy.connectedDevices.toList()
                        } catch (_: SecurityException) {
                            emptyList()
                        }
                    currentAdapter.closeProfileProxy(connectedProfile, proxy)
                    onResult(devices)
                }

                override fun onServiceDisconnected(disconnectedProfile: Int) = Unit
            },
            profile,
        )
    }

    fun close() {
        if (closed) return
        closed = true
        try {
            ctx.unregisterReceiver(receiver)
        } catch (_: IllegalArgumentException) {}
    }

    @Suppress("DEPRECATION")
    private fun Intent.bluetoothDevice(): BluetoothDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
}
