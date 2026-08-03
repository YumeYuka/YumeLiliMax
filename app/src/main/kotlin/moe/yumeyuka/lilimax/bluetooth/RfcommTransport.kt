package moe.yumeyuka.lilimax.bluetooth

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import java.io.InputStream
import java.io.OutputStream
import java.util.*

interface RfcommTransport {
    val device: BluetoothDevice

    fun connect()

    fun read(buffer: ByteArray): Int

    fun write(data: ByteArray)

    fun close()
}

class AndroidRfcommTransport(
    override val device: BluetoothDevice,
    uuid: UUID,
) : RfcommTransport {
    private val socket: BluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
    private var input: InputStream? = null
    private var output: OutputStream? = null

    override fun connect() {
        socket.connect()
        input = socket.inputStream
        output = socket.outputStream
    }

    override fun read(buffer: ByteArray): Int =
        requireNotNull(input) { "Transport is not connected" }.read(buffer)

    override fun write(data: ByteArray) {
        requireNotNull(output) { "Transport is not connected" }
            .run {
                write(data)
                flush()
            }
    }

    override fun close() {
        try {
            input?.close()
        } catch (_: Exception) {}
        try {
            output?.close()
        } catch (_: Exception) {}
        try {
            socket.close()
        } catch (_: Exception) {}
        input = null
        output = null
    }
}

fun interface RfcommTransportFactory {
    fun create(device: BluetoothDevice, uuid: UUID): RfcommTransport
}
