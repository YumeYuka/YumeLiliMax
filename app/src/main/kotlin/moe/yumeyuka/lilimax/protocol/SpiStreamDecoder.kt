package moe.yumeyuka.lilimax.protocol

class SpiStreamDecoder(
    private val maxPayloadLength: Int = 255,
    private val maxBufferSize: Int = 4096,
) {
    private val buffer = ArrayList<Byte>()

    fun append(data: ByteArray): List<SpiPacket> {
        data.forEach { byte ->
            if (buffer.size >= maxBufferSize) buffer.removeAt(0)
            buffer += byte
        }

        val packets = mutableListOf<SpiPacket>()
        while (buffer.size >= MIN_FRAME_SIZE) {
            val headerIndex = findHeader()
            if (headerIndex < 0) {
                retainPossibleHeaderPrefix()
                break
            }
            repeat(headerIndex) { buffer.removeAt(0) }
            if (buffer.size < MIN_FRAME_SIZE) break

            val payloadLength = buffer[4].toInt() and 0xFF
            if (payloadLength > maxPayloadLength) {
                buffer.removeAt(0)
                continue
            }
            val frameLength = MIN_FRAME_SIZE + payloadLength
            if (buffer.size < frameLength) break

            val sequence = buffer[0]
            val command = buffer[1]
            val payload = ByteArray(payloadLength) { index -> buffer[MIN_FRAME_SIZE + index] }
            repeat(frameLength) { buffer.removeAt(0) }
            packets += SpiPacket(sequence, command, payload)
        }
        return packets
    }

    fun reset() = buffer.clear()

    val bufferedByteCount: Int
        get() = buffer.size

    private fun findHeader(): Int {
        for (index in 0..buffer.size - MIN_FRAME_SIZE) {
            if (
                (buffer[index + 2] == SpiConstants.HEADER_FIXED_0 ||
                    buffer[index + 2] == SpiConstants.RESPONSE_HEADER_FIXED_0) &&
                    buffer[index + 3] == SpiConstants.HEADER_FIXED_1
            )
                return index
        }
        return -1
    }

    private fun retainPossibleHeaderPrefix() {
        while (buffer.size > 3) buffer.removeAt(0)
    }

    private companion object {
        const val MIN_FRAME_SIZE = 5
    }
}
