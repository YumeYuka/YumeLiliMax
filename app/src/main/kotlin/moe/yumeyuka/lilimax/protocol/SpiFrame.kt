package moe.yumeyuka.lilimax.protocol

object SpiFrame {

    fun buildGetInfo(subType: Byte): ByteArray =
        byteArrayOf(
            0x00,
            SpiConstants.CMD_GET_INFO,
            SpiConstants.HEADER_FIXED_0,
            SpiConstants.HEADER_FIXED_1,
            0x01,
            subType,
        )

    fun buildSet(cmd: Byte, payload: ByteArray): ByteArray =
        SpiPacket(sequence = 0x00, command = cmd, payload = payload).encode()

    fun buildAudioProtocol(protocol: Byte) =
        buildSet(SpiConstants.CMD_AUDIO_PROTOCOL, byteArrayOf(protocol))

    fun buildNoiseControl(mode: Byte) = buildSet(SpiConstants.CMD_NOISE_CONTROL, byteArrayOf(mode))

    fun buildWorkMode(mode: Byte) = buildSet(SpiConstants.CMD_WORK_MODE, byteArrayOf(mode))

    fun buildSoundEffects(mode: Byte) =
        buildSet(SpiConstants.CMD_SOUND_EFFECTS, byteArrayOf(0x00, mode))

    fun buildLanguage(lang: Byte) = buildSet(SpiConstants.CMD_LANGUAGE, byteArrayOf(lang))

    fun buildFinder(target: Byte) = buildSet(SpiConstants.CMD_FINDER, byteArrayOf(target))

    fun buildAlertVolume(volume: Byte) =
        buildSet(SpiConstants.CMD_ALERT_VOLUME, byteArrayOf(volume))

    fun buildPowerOff() = buildSet(SpiConstants.CMD_POWER_OFF, byteArrayOf(0x00))

    fun buildReset() = buildSet(SpiConstants.CMD_RESET, byteArrayOf())

    fun buildClearPairing() = buildSet(SpiConstants.CMD_CLEAR_PAIR, byteArrayOf())

    fun toHex(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): String =
        buildString(length * 3) {
            for (i in offset until offset + length) {
                if (i > offset) append(' ')
                append(String.format("%02X", data[i]))
            }
        }

    fun fromHex(hex: String): ByteArray =
        hex.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.toInt(16).toByte() }
            .toByteArray()
}
