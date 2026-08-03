package moe.yumeyuka.lilimax.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import moe.yumeyuka.lilimax.model.BluetoothEvent
import moe.yumeyuka.lilimax.model.ConnectionState
import moe.yumeyuka.lilimax.model.DisconnectReason
import moe.yumeyuka.lilimax.protocol.SpiConstants
import moe.yumeyuka.lilimax.protocol.SpiFrame
import java.io.IOException
import java.util.*
import kotlin.time.Duration.Companion.milliseconds

interface DeviceConnector {
    val state: StateFlow<ConnectionState>
    val connectedDevice: StateFlow<BluetoothDevice?>
    val events: SharedFlow<BluetoothEvent>

    fun connect(device: BluetoothDevice)

    fun disconnect(reason: DisconnectReason = DisconnectReason.User)

    fun send(data: ByteArray): Boolean

    fun sendHex(hex: String): Boolean = send(SpiFrame.fromHex(hex))

    fun close()
}

@SuppressLint("MissingPermission")
class BtDeviceConnector(
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val transportFactory: RfcommTransportFactory =
        RfcommTransportFactory(::AndroidRfcommTransport),
) : DeviceConnector {
    companion object {
        private const val TAG = "BtConnector"
        private const val CONNECT_TIMEOUT_MS = 15_000L
        val SPP_UUID: UUID = UUID.fromString(SpiConstants.SPP_UUID)
    }

    private sealed interface Command {
        data class Connect(val device: BluetoothDevice) : Command

        data class ConnectSucceeded(
            val device: BluetoothDevice,
            val transport: RfcommTransport,
        ) : Command

        data class ConnectFailed(
            val device: BluetoothDevice,
            val transport: RfcommTransport,
            val message: String?,
        ) : Command

        data class Disconnect(val reason: DisconnectReason) : Command

        data class Send(val data: ByteArray) : Command {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Send

                return data.contentEquals(other.data)
            }

            override fun hashCode(): Int {
                return data.contentHashCode()
            }
        }

        data class ReaderEnded(val transport: RfcommTransport) : Command

        data class ReaderFailed(val transport: RfcommTransport, val message: String?) : Command

        data object Close : Command
    }

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val commands = Channel<Command>(Channel.UNLIMITED)
    private var transport: RfcommTransport? = null
    private var connectJob: Job? = null
    private var readerJob: Job? = null
    private var currentDevice: BluetoothDevice? = null
    @Volatile private var closed = false

    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    override val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    override val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()

    private val _events = MutableSharedFlow<BluetoothEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<BluetoothEvent> = _events

    init {
        scope.launch {
            for (command in commands) {
                when (command) {
                    is Command.Connect -> beginConnect(command.device)
                    is Command.ConnectSucceeded -> finishConnect(command)
                    is Command.ConnectFailed -> failConnect(command)
                    is Command.Disconnect -> cleanup(command.reason, transport != null)
                    is Command.Send -> handleSend(command.data)
                    is Command.ReaderEnded ->
                        if (transport === command.transport) {
                            cleanup(DisconnectReason.RemoteClosed, emitDisconnected = true)
                        }
                    is Command.ReaderFailed ->
                        if (transport === command.transport) {
                            _events.emit(BluetoothEvent.ReceiveError(command.message))
                            cleanup(DisconnectReason.TransportError, emitDisconnected = true)
                        }
                    Command.Close -> {
                        cleanup(DisconnectReason.ManagerClosed, transport != null)
                        closed = true
                        _state.value = ConnectionState.Closed
                        commands.close()
                    }
                }
            }
        }
    }

    override fun connect(device: BluetoothDevice) {
        if (!closed) commands.trySend(Command.Connect(device))
    }

    override fun disconnect(reason: DisconnectReason) {
        if (!closed) commands.trySend(Command.Disconnect(reason))
    }

    override fun send(data: ByteArray): Boolean =
        !closed &&
            state.value is ConnectionState.Connected &&
            commands.trySend(Command.Send(data.copyOf())).isSuccess

    override fun close() {
        if (!closed) commands.trySend(Command.Close)
    }

    private suspend fun beginConnect(device: BluetoothDevice) {
        if (closed) return
        if (
            currentDevice?.address == device.address &&
                (_state.value is ConnectionState.Connecting ||
                    _state.value is ConnectionState.Connected)
        )
            return

        cleanup(DisconnectReason.Replaced, transport != null)
        currentDevice = device
        _state.value = ConnectionState.Connecting(device)
        delay(300L)
        if (closed) return
        val candidate = transportFactory.create(device, SPP_UUID)
        transport = candidate
        connectJob = scope.launch {
            try {
                withTimeout(CONNECT_TIMEOUT_MS.milliseconds) { candidate.connect() }
                commands.send(Command.ConnectSucceeded(device, candidate))
            } catch (error: Exception) {
                commands.send(Command.ConnectFailed(device, candidate, error.message))
            }
        }
    }

    private suspend fun finishConnect(command: Command.ConnectSucceeded) {
        if (transport !== command.transport || closed) {
            command.transport.close()
            return
        }
        connectJob = null
        currentDevice = command.device
        _connectedDevice.value = command.device
        _state.value = ConnectionState.Connected(command.device)
        _events.emit(BluetoothEvent.Connected(command.device))
        startReader(command.transport)
        Log.d(TAG, "Connected: ${command.device.address}")
    }

    private suspend fun failConnect(command: Command.ConnectFailed) {
        if (transport !== command.transport) return
        connectJob = null
        command.transport.close()
        transport = null
        currentDevice = null
        _connectedDevice.value = null
        _state.value = ConnectionState.Failed(command.device, command.message)
        _events.emit(BluetoothEvent.ConnectionFailed(command.device, command.message))
        Log.e(TAG, "Connect failed: ${command.message}")
    }

    private suspend fun handleSend(data: ByteArray) {
        val active = transport ?: return
        if (_state.value !is ConnectionState.Connected) return
        try {
            active.write(data)
            Log.d(TAG, "TX: ${SpiFrame.toHex(data)}")
        } catch (error: IOException) {
            _events.emit(BluetoothEvent.SendError(error.message))
            cleanup(DisconnectReason.TransportError, emitDisconnected = true)
        }
    }

    private fun startReader(active: RfcommTransport) {
        readerJob?.cancel()
        readerJob = scope.launch {
            val buffer = ByteArray(1024)
            try {
                while (transport === active) {
                    val count = active.read(buffer)
                    if (count < 0) {
                        commands.send(Command.ReaderEnded(active))
                        break
                    }
                    if (count > 0) {
                        val chunk = buffer.copyOf(count)
                        _events.emit(BluetoothEvent.DataReceived(chunk, SpiFrame.toHex(chunk)))
                    }
                }
            } catch (error: IOException) {
                if (transport === active) commands.send(Command.ReaderFailed(active, error.message))
            }
        }
    }

    private suspend fun cleanup(reason: DisconnectReason, emitDisconnected: Boolean) {
        val previousDevice = currentDevice
        val active = transport
        if (active == null && previousDevice == null) {
            if (!closed && _state.value !is ConnectionState.Failed)
                _state.value = ConnectionState.Idle
            return
        }
        _state.value = ConnectionState.Disconnecting(previousDevice)
        transport = null
        connectJob?.cancel()
        connectJob = null
        readerJob?.cancel()
        readerJob = null
        active?.close()
        currentDevice = null
        _connectedDevice.value = null
        if (!closed) _state.value = ConnectionState.Idle
        if (emitDisconnected) _events.emit(BluetoothEvent.Disconnected(reason))
    }
}
