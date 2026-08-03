package moe.yumeyuka.lilimax.model

import android.bluetooth.BluetoothDevice

sealed interface ConnectionState {
    data object Idle : ConnectionState

    data class Connecting(val device: BluetoothDevice) : ConnectionState

    data class Connected(val device: BluetoothDevice) : ConnectionState

    data class Disconnecting(val device: BluetoothDevice?) : ConnectionState

    data class Failed(val device: BluetoothDevice, val message: String?) : ConnectionState

    data object Closed : ConnectionState
}

enum class DisconnectReason {
    User,
    Replaced,
    RemoteClosed,
    TransportError,
    ManagerClosed,
}
