package moe.yumeyuka.lilimax.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.StateFlow
import moe.yumeyuka.lilimax.BluetoothManager
import moe.yumeyuka.lilimax.model.ConnectionState
import moe.yumeyuka.lilimax.model.EarbudState
import moe.yumeyuka.lilimax.model.RawRxInfo

class ViewModel(private val manager: BluetoothManager) : ViewModel() {

    val events
        get() = manager.events

    val connectionState: StateFlow<ConnectionState>
        get() = manager.connectionState

    val earbudState: StateFlow<EarbudState>
        get() = manager.earbudState

    val rawRx: StateFlow<RawRxInfo>
        get() = manager.rawRx

    fun connectToHeadset() = manager.scanAndConnect()

    fun disconnect() = manager.disconnect()

    fun queryAll() = manager.queryAll()

    fun switchAudioProtocol(protocol: Byte) = manager.switchAudioProtocol(protocol)

    fun switchNoiseControl(mode: Byte) = manager.switchNoiseControl(mode)

    fun switchWorkMode(mode: Byte) = manager.switchWorkMode(mode)

    fun switchEq(mode: Byte) = manager.switchEq(mode)

    fun switchLanguage(lang: Byte) = manager.switchLanguage(lang)

    fun switchFinder(target: Byte) = manager.switchFinder(target)

    fun setAlertVolume(volume: Byte) = manager.setAlertVolume(volume)

    fun powerOff() = manager.powerOff()

    fun resetDevice() = manager.resetDevice()

    fun clearPairing() = manager.clearPairing()

    class Factory(private val manager: BluetoothManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ViewModel(manager) as T
    }
}
