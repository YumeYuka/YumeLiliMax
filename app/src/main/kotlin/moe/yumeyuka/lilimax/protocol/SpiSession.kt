package moe.yumeyuka.lilimax.protocol

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import moe.yumeyuka.lilimax.model.EarbudState

enum class SpiSessionState {
    Initializing,
    Ready,
    Closed,
}

sealed interface SpiSessionEvent {
    data class CommandFailed(val command: Byte, val message: String) : SpiSessionEvent

    data object TransportInactive : SpiSessionEvent

    data object AudioProtocolReconnectRequired : SpiSessionEvent
}

class SpiSession(
    private val profile: ProtocolProfile,
    private val scope: CoroutineScope,
    private val sendFrame: (ByteArray) -> Boolean,
) {
    private val decoder = SpiStreamDecoder()
    private var sequence = 0
    private var heartbeatJob: Job? = null
    private var queryOrder = byteArrayOf()
    private var queryIndex = 0
    private var pendingQuery: Byte? = null
    private var pendingAttempts = 0
    private var lastQuerySentAtMs = 0L
    private var pauseQueriesUntilMs = 0L
    private var lastReceivedAtMs = System.currentTimeMillis()
    private var receivedAny = false

    private val _state = MutableStateFlow(SpiSessionState.Initializing)
    val state: StateFlow<SpiSessionState> = _state.asStateFlow()

    private val _earbudState = MutableStateFlow(EarbudState())
    val earbudState: StateFlow<EarbudState> = _earbudState.asStateFlow()

    private val _events = MutableSharedFlow<SpiSessionEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<SpiSessionEvent> = _events

    fun start() {
        if (_state.value == SpiSessionState.Closed) return
        _state.value = SpiSessionState.Ready
        heartbeatJob?.cancel()
        queryOrder = queryOrder(profile)
        queryIndex = 0
        pendingQuery = null
        pendingAttempts = 0
        lastQuerySentAtMs = 0L
        pauseQueriesUntilMs = 0L
        heartbeatJob = scope.launch {
            while (_state.value != SpiSessionState.Closed) {
                val now = System.currentTimeMillis()
                if (now >= pauseQueriesUntilMs) {
                    val pending = pendingQuery
                    if (pending == null) {
                        val subtype = queryOrder[queryIndex % queryOrder.size]
                        if (!sendQuery(subtype)) {
                            _events.emit(
                                SpiSessionEvent.CommandFailed(
                                    SpiConstants.CMD_GET_INFO,
                                    "Transport rejected query",
                                )
                            )
                            return@launch
                        }
                        pendingQuery = subtype
                        pendingAttempts = 1
                        lastQuerySentAtMs = now
                    } else if (now - lastQuerySentAtMs >= QUERY_RETRY_INTERVAL_MS) {
                        if (pendingAttempts < MAX_QUERY_ATTEMPTS) {
                            if (!sendQuery(pending)) {
                                _events.emit(
                                    SpiSessionEvent.CommandFailed(
                                        SpiConstants.CMD_GET_INFO,
                                        "Transport rejected query retry",
                                    )
                                )
                                return@launch
                            }
                            pendingAttempts++
                            lastQuerySentAtMs = now
                        } else {
                            pendingQuery = null
                            pendingAttempts = 0
                            queryIndex++
                        }
                    }
                }

                delay(QUERY_TICK_MS)
                val idleMs = System.currentTimeMillis() - lastReceivedAtMs
                if (receivedAny && idleMs >= profile.inactivityTimeoutMs) {
                    _events.emit(SpiSessionEvent.TransportInactive)
                    lastReceivedAtMs = System.currentTimeMillis()
                }
            }
        }
    }

    fun onBytes(bytes: ByteArray): Int {
        if (_state.value == SpiSessionState.Closed) return 0
        val packets = decoder.append(bytes)
        if (packets.isEmpty()) return 0
        lastReceivedAtMs = System.currentTimeMillis()
        receivedAny = true
        packets.forEach { packet ->
            Log.d(TAG, "RX: ${SpiFrame.toHex(packet.encode())}")
            _earbudState.value = SpiResponseParser.reduce(_earbudState.value, packet, profile)
            if (packet.command == SpiConstants.CMD_GET_INFO && packet.payload.isNotEmpty()) {
                val subtype = packet.payload[0]
                if (subtype == pendingQuery) {
                    pendingQuery = null
                    pendingAttempts = 0
                    queryIndex++
                }
            }
        }
        return packets.size
    }

    fun sendQuery(subtype: Byte): Boolean =
        sendFrame(
            SpiPacket(nextSequence(), SpiConstants.CMD_GET_INFO, byteArrayOf(subtype)).encode()
        )

    fun sendSet(command: Byte, payload: ByteArray): Boolean =
        sendFrame(SpiPacket(nextSequence(), command, payload).encode()).also {
            if (it) {
                pendingQuery = null
                pendingAttempts = 0
                pauseQueriesUntilMs = System.currentTimeMillis() + SETTLE_AFTER_SET_MS
            }
        }

    fun close() {
        if (_state.value == SpiSessionState.Closed) return
        heartbeatJob?.cancel()
        heartbeatJob = null
        decoder.reset()
        _state.value = SpiSessionState.Closed
    }

    private fun nextSequence(): Byte =
        when (profile.sequencePolicy) {
            SequencePolicy.FixedZero -> 0
            SequencePolicy.Incrementing -> (sequence++ and 0xFF).toByte()
        }

    private companion object {
        const val TAG = "SpiSession"
        const val QUERY_TICK_MS = 50L
        const val QUERY_RETRY_INTERVAL_MS = 400L
        const val MAX_QUERY_ATTEMPTS = 3
        const val SETTLE_AFTER_SET_MS = 450L

        fun queryOrder(profile: ProtocolProfile): ByteArray =
            byteArrayOf(
                SpiConstants.SUB_PACKAGE_LEN,
                SpiConstants.SUB_BATTERY,
                SpiConstants.SUB_VERSION,
                SpiConstants.SUB_NAME,
                SpiConstants.SUB_EQ_MODE,
                SpiConstants.SUB_KEY_MODE,
                SpiConstants.SUB_WORK_MODE,
                SpiConstants.SUB_LANGUAGE,
                SpiConstants.SUB_NOISE_CONTROL,
                SpiConstants.SUB_ALERT_VOLUME,
                SpiConstants.SUB_TOUCH_SWITCH,
                profile.audioProtocolQuerySubtype,
                SpiConstants.SUB_ANC_SWITCH,
                SpiConstants.SUB_FINDER,
            )
    }
}
