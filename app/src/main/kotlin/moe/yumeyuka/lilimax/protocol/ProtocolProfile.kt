package moe.yumeyuka.lilimax.protocol

import moe.yumeyuka.lilimax.model.DeviceModel
import moe.yumeyuka.lilimax.model.ProtocolFamily

enum class SequencePolicy {
    FixedZero,
    Incrementing,
}

data class ProtocolProfile(
    val sequencePolicy: SequencePolicy,
    val audioProtocolQuerySubtype: Byte,
    val heartbeatIntervalMs: Long,
    val inactivityTimeoutMs: Long,
    val disconnectAfterAudioSwitch: Boolean,
)

object ProtocolProfileFactory {
    fun forModel(model: DeviceModel): ProtocolProfile =
        when (model.protocolFamily) {
            ProtocolFamily.M8 ->
                ProtocolProfile(
                    sequencePolicy = SequencePolicy.FixedZero,
                    audioProtocolQuerySubtype = SpiConstants.SUB_AUDIO_PROTOCOL,
                    heartbeatIntervalMs = 250,
                    inactivityTimeoutMs = 1_000,
                    disconnectAfterAudioSwitch = false,
                )
            ProtocolFamily.M3,
            ProtocolFamily.AB ->
                ProtocolProfile(
                    sequencePolicy = SequencePolicy.Incrementing,
                    audioProtocolQuerySubtype = SpiConstants.SUB_AUDIO_PROTOCOL_PUSH,
                    heartbeatIntervalMs = 500,
                    inactivityTimeoutMs = 2_500,
                    disconnectAfterAudioSwitch = true,
                )
        }
}
