package moe.yumeyuka.lilimax.protocol

class SpiPacket(
    val sequence: Byte,
    val command: Byte,
    payload: ByteArray,
) {
    val payload: ByteArray = payload.copyOf()

    fun encode(): ByteArray =
        ByteArray(5 + payload.size).also { frame ->
            frame[0] = sequence
            frame[1] = command
            frame[2] = SpiConstants.HEADER_FIXED_0
            frame[3] = SpiConstants.HEADER_FIXED_1
            frame[4] = payload.size.toByte()
            payload.copyInto(frame, destinationOffset = 5)
        }

    override fun equals(other: Any?): Boolean =
        other is SpiPacket &&
            sequence == other.sequence &&
            command == other.command &&
            payload.contentEquals(other.payload)

    override fun hashCode(): Int = 31 * (31 * sequence + command) + payload.contentHashCode()
}
