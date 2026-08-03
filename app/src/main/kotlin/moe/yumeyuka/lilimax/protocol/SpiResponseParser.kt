package moe.yumeyuka.lilimax.protocol

import moe.yumeyuka.lilimax.model.BatteryState
import moe.yumeyuka.lilimax.model.EarbudState

object SpiResponseParser {
    fun reduce(current: EarbudState, packet: SpiPacket, profile: ProtocolProfile): EarbudState {
        if (
            packet.command != SpiConstants.CMD_GET_INFO &&
                packet.command != SpiConstants.CMD_PUSH_NOTIFY
        ) {
            return current
        }
        val payload = packet.payload
        if (payload.isEmpty()) return current
        val subtype = payload[0]
        return when (subtype) {
            SpiConstants.SUB_BATTERY -> parseBattery(current, payload)
            SpiConstants.SUB_VERSION -> parseVersion(current, payload, profile)
            SpiConstants.SUB_NAME -> parseName(current, payload)
            SpiConstants.SUB_EQ_MODE -> current.copy(eqMode = payload.valueAt(3, 2, 1))
            SpiConstants.SUB_ANC_SWITCH ->
                current.copy(ancEnabled = payload.valueAt(2, 1)?.let { it != 0 })
            SpiConstants.SUB_WORK_MODE -> current.copy(workMode = payload.valueAt(2, 1))
            SpiConstants.SUB_LANGUAGE -> current.copy(language = payload.valueAt(2, 1))
            SpiConstants.SUB_AUDIO_PROTOCOL,
            SpiConstants.SUB_AUDIO_PROTOCOL_PUSH ->
                current.copy(audioProtocol = payload.valueAt(2, 1))
            SpiConstants.SUB_NOISE_CONTROL -> current.copy(noiseControl = payload.valueAt(2, 1))
            SpiConstants.SUB_FINDER -> current.copy(finderMode = payload.valueAt(2, 1))
            SpiConstants.SUB_TOUCH_SWITCH ->
                current.copy(touchEnabled = payload.valueAt(2, 1)?.let { it != 0 })
            SpiConstants.SUB_ALERT_VOLUME -> current.copy(alertVolume = payload.valueAt(2, 1))
            else -> current
        }
    }

    private fun parseBattery(current: EarbudState, payload: ByteArray): EarbudState {
        val offset = if (payload.size >= 5) 2 else 1
        if (payload.size < offset + 3) return current
        val left = payload[offset].toInt() and 0xFF
        val right = payload[offset + 1].toInt() and 0xFF
        val case = payload[offset + 2].toInt() and 0xFF
        return current.copy(
            battery =
                BatteryState(
                    leftPercent = left and 0x7F,
                    rightPercent = right and 0x7F,
                    casePercent = case and 0x7F,
                    leftCharging = left and 0x80 != 0,
                    rightCharging = right and 0x80 != 0,
                )
        )
    }

    private fun parseVersion(
        current: EarbudState,
        payload: ByteArray,
        profile: ProtocolProfile,
    ): EarbudState {
        val start =
            when {
                payload.size >= 6 -> 2
                payload.size >= 5 -> 1
                else -> return current
            }
        val bytes = payload.copyOfRange(start, start + 4)
        val ordered =
            if (profile.sequencePolicy == SequencePolicy.FixedZero) bytes else bytes.reversedArray()
        var version = 0L
        ordered.forEach { version = (version shl 8) or (it.toLong() and 0xFF) }
        return current.copy(firmwareVersion = version)
    }

    private fun parseName(current: EarbudState, payload: ByteArray): EarbudState {
        for (lengthIndex in intArrayOf(1, 2)) {
            val length = payload.valueAt(lengthIndex) ?: continue
            val nameStart = lengthIndex + 1
            if (payload.size >= nameStart + length) {
                return current.copy(
                    bluetoothName =
                        payload.copyOfRange(nameStart, nameStart + length).toString(Charsets.UTF_8)
                )
            }
        }
        return current
    }

    private fun ByteArray.valueAt(vararg indices: Int): Int? {
        for (index in indices) {
            getOrNull(index)?.let {
                return it.toInt().and(0xFF)
            }
        }
        return null
    }
}
