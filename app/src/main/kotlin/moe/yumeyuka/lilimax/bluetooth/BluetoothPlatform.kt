package moe.yumeyuka.lilimax.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import moe.yumeyuka.lilimax.model.DeviceModel
import android.bluetooth.BluetoothManager as AndroidBluetoothManager

@SuppressLint("MissingPermission")
class BluetoothPlatform(context: Context) {
    val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager)?.adapter

    val isAvailable: Boolean
        get() = adapter != null

    val isEnabled: Boolean
        get() = adapter?.isEnabled == true

    fun bondedMishuaiDevices(): List<BluetoothDevice> =
        adapter
            ?.bondedDevices
            ?.filter {
                DeviceModel.fromBluetoothName(it.name) != null ||
                    DeviceModel.looksLikeMishuaiName(it.name)
            }
            ?.sortedBy { it.name }
            .orEmpty()
}
