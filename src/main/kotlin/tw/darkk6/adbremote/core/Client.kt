package tw.darkk6.adbremote.core

import tw.darkk6.adbremote.app.Constants
import tw.darkk6.adbremote.util.Logger
import java.net.InetSocketAddress
import java.net.Socket

sealed class Client(
    private val clientName: String,
    private val host: String,
    private val port: Int
) {
    class App(host: String, port: Int) : Client("App", host, port)

    class ADB(port: Int = Constants.ADB_DEFAULT_PORT) : Client("ADB", Constants.LOCAL_HOST, port)

    private var isConnected = false

    fun connect(onConnected: (Socket) -> Unit): Boolean {
        return isConnected || try {
            val address = InetSocketAddress(host, port)
            val socket = Socket()
            socket.connect(address, Constants.CONNECT_TIMEOUT_MS)
            Logger.b("$clientName client connected to $host:$port")
            onConnected(socket)
            isConnected = true
            true
        } catch (e: Exception) {
            Logger.e("$clientName client connect fail", e)
            false
        }
    }
}