package moe.yumeyuka.lilimax

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import moe.yumeyuka.lilimax.bluetooth.*
import moe.yumeyuka.lilimax.model.*
import moe.yumeyuka.lilimax.protocol.ProtocolProfileFactory
import moe.yumeyuka.lilimax.protocol.SpiConstants
import moe.yumeyuka.lilimax.protocol.SpiFrame
import moe.yumeyuka.lilimax.protocol.SpiSession

class BluetoothManager(context: Context) {
    private val ctx = context.applicationContext
    private val platform = BluetoothPlatform(ctx)
    private val profiles = BluetoothProfileCoordinator(ctx)
    val connector: DeviceConnector = BtDeviceConnector()
    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var spiSession: SpiSession? = null
    private var spiCollectorJob: Job? = null
    private var reconnectJob: Job? = null
    private var userDisconnected = false
    private var closed = false

    val connectionState: StateFlow<ConnectionState>
        get() = connector.state

    val connectedDevice
        get() = connector.connectedDevice

    private val _earbudState = MutableStateFlow(EarbudState())
    val earbudState: StateFlow<EarbudState> = _earbudState.asStateFlow()

    private val _rawRx = MutableStateFlow(RawRxInfo())
    val rawRx: StateFlow<RawRxInfo> = _rawRx.asStateFlow()

    private val _events = MutableSharedFlow<BluetoothEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BluetoothEvent> = _events

    fun hasPermissions() = PermissionHelper.allGranted(ctx)

    fun missingPermissions() = PermissionHelper.missingPermissions(ctx)

    fun isBluetoothEnabled(): Boolean = platform.isEnabled

    fun scanAndConnect() {
        if (closed || !hasPermissions() || !isBluetoothEnabled()) return
        userDisconnected = false
        reconnectJob?.cancel()
        val state = connector.state.value
        if (state is ConnectionState.Connecting || state is ConnectionState.Connected) return
        profiles.findConnectedAudioDevices()
        platform.bondedMishuaiDevices().firstOrNull()?.let(::connect)
    }

    fun connect(device: BluetoothDevice) {
        if (closed || !hasPermissions() || !isBluetoothEnabled()) return
        reconnectJob?.cancel()
        connector.connect(device)
    }

    fun disconnect() {
        userDisconnected = true
        reconnectJob?.cancel()
        closeSpiSession()
        connector.disconnect(DisconnectReason.User)
    }

    fun queryAll() =
        spiSession?.let {
            it.start()
            true
        } ?: connector.send(SpiFrame.buildGetInfo(SpiConstants.SUB_PACKAGE_LEN))

    fun switchAudioProtocol(protocol: Byte) =
        spiSession?.sendSet(SpiConstants.CMD_AUDIO_PROTOCOL, byteArrayOf(protocol))
            ?: connector.send(SpiFrame.buildAudioProtocol(protocol))

    fun switchNoiseControl(mode: Byte) =
        spiSession?.sendSet(SpiConstants.CMD_NOISE_CONTROL, byteArrayOf(mode))
            ?: connector.send(SpiFrame.buildNoiseControl(mode))

    fun switchWorkMode(mode: Byte) =
        spiSession?.sendSet(SpiConstants.CMD_WORK_MODE, byteArrayOf(mode))
            ?: connector.send(SpiFrame.buildWorkMode(mode))

    fun switchEq(mode: Byte) =
        spiSession?.sendSet(SpiConstants.CMD_SOUND_EFFECTS, byteArrayOf(0x00, mode))
            ?: connector.send(SpiFrame.buildSoundEffects(mode))

    fun switchLanguage(lang: Byte) =
        spiSession?.sendSet(SpiConstants.CMD_LANGUAGE, byteArrayOf(lang))
            ?: connector.send(SpiFrame.buildLanguage(lang))

    fun switchFinder(target: Byte) =
        spiSession?.sendSet(SpiConstants.CMD_FINDER, byteArrayOf(target))
            ?: connector.send(SpiFrame.buildFinder(target))

    fun setAlertVolume(volume: Byte) =
        spiSession?.sendSet(SpiConstants.CMD_ALERT_VOLUME, byteArrayOf(volume))
            ?: connector.send(SpiFrame.buildAlertVolume(volume))

    fun powerOff() =
        spiSession?.sendSet(SpiConstants.CMD_POWER_OFF, byteArrayOf(0x00))
            ?: connector.send(SpiFrame.buildPowerOff())

    fun resetDevice() =
        spiSession?.sendSet(SpiConstants.CMD_RESET, byteArrayOf())
            ?: connector.send(SpiFrame.buildReset())

    fun clearPairing() =
        spiSession?.sendSet(SpiConstants.CMD_CLEAR_PAIR, byteArrayOf())
            ?: connector.send(SpiFrame.buildClearPairing())

    init {
        scope.launch {
            connector.events.collect { event ->
                when (event) {
                    is BluetoothEvent.Connected -> startSpiSession(event.device)
                    is BluetoothEvent.DataReceived -> {
                        val packets = spiSession?.onBytes(event.data) ?: 0
                        _rawRx.value =
                            RawRxInfo(
                                totalBytes = _rawRx.value.totalBytes + event.data.size,
                                lastHex = event.hex.take(160),
                                decodedPackets = _rawRx.value.decodedPackets + packets,
                            )
                    }
                    is BluetoothEvent.Disconnected,
                    is BluetoothEvent.ConnectionFailed -> closeSpiSession()
                    else -> Unit
                }
                _events.emit(event)
            }
        }
        scope.launch { profiles.events.collect(::handleProfileEvent) }
        if (hasPermissions() && isBluetoothEnabled()) scanAndConnect()
    }

    private suspend fun handleProfileEvent(event: BluetoothProfileEvent) {
        when (event) {
            is BluetoothProfileEvent.CurrentAudioDevice -> onDeviceAvailable(event.device)
            is BluetoothProfileEvent.AclConnected,
            is BluetoothProfileEvent.A2dpConnected -> maybeReconnect(event.device)
            is BluetoothProfileEvent.AclDisconnected,
            is BluetoothProfileEvent.A2dpDisconnected -> {
                if (connectedDevice.value?.address == event.device.address) {
                    closeSpiSession()
                    connector.disconnect(DisconnectReason.RemoteClosed)
                }
            }
        }
    }

    private fun onDeviceAvailable(device: BluetoothDevice) {
        if (closed || userDisconnected) return
        if (
            DeviceModel.fromBluetoothName(device.name) == null &&
                !DeviceModel.looksLikeMishuaiName(device.name)
        ) {
            Log.d(TAG, "ignore unmatched device: ${device.name}")
            return
        }
        if (connector.state.value !is ConnectionState.Idle) return
        Log.d(TAG, "matched device: ${device.name}")
        connect(device)
    }

    private fun startSpiSession(device: BluetoothDevice) {
        val model =
            DeviceModel.fromBluetoothName(device.name)
                ?: DeviceModel.GLAZE_MAX.also {
                    Log.w(TAG, "unknown device name '${device.name}', fallback to GLAZE_MAX(M8)")
                }
        closeSpiSession()
        val session =
            SpiSession(
                profile = ProtocolProfileFactory.forModel(model),
                scope = scope,
                sendFrame = connector::send,
            )
        spiSession = session
        spiCollectorJob = scope.launch { session.earbudState.collect { _earbudState.value = it } }
        session.start()
    }

    private fun maybeReconnect(device: BluetoothDevice) {
        if (closed || userDisconnected || connector.state.value !is ConnectionState.Idle) return
        if (
            DeviceModel.fromBluetoothName(device.name) == null &&
                !DeviceModel.looksLikeMishuaiName(device.name)
        )
            return
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            val delays = longArrayOf(250, 500, 1_000, 2_000, 4_000)
            for (delayMs in delays) {
                if (userDisconnected || closed) return@launch
                delay(delayMs)
                val state = connector.state.value
                if (state is ConnectionState.Connected || state is ConnectionState.Connecting)
                    return@launch
                connector.connect(device)
            }
        }
    }

    private fun closeSpiSession() {
        spiCollectorJob?.cancel()
        spiCollectorJob = null
        spiSession?.close()
        spiSession = null
        _earbudState.value = EarbudState()
    }

    fun close() {
        if (closed) return
        closed = true
        reconnectJob?.cancel()
        closeSpiSession()
        connector.close()
        profiles.close()
        scope.cancel()
    }

    private companion object {
        const val TAG = "BtManager"
    }
}
