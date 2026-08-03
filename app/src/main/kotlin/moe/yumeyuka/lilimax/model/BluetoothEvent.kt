package moe.yumeyuka.lilimax.model

import android.bluetooth.BluetoothDevice

sealed class BluetoothEvent {
    data class Connected(val device: BluetoothDevice) : BluetoothEvent()

    data class Disconnected(val reason: DisconnectReason) : BluetoothEvent()

    data class ConnectionFailed(val device: BluetoothDevice, val error: String?) : BluetoothEvent()

    data class DataReceived(val data: ByteArray, val hex: String) : BluetoothEvent() {
        override fun equals(other: Any?): Boolean =
            other is DataReceived && data.contentEquals(other.data) && hex == other.hex

        override fun hashCode(): Int = 31 * data.contentHashCode() + hex.hashCode()
    }

    data class ReceiveError(val error: String?) : BluetoothEvent()

    data class SendError(val error: String?) : BluetoothEvent()
}
