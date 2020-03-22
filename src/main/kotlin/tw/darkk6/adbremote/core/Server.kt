package tw.darkk6.adbremote.core

import tw.darkk6.adbremote.core.ktx.safeClose
import tw.darkk6.adbremote.util.Logger
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.withLock
import kotlin.concurrent.write

sealed class Server(
    private val serverName: String,
    private val port: Int,
    private val onConnected: (socket: Socket) -> Unit
) : Runnable {

    class App(
        port: Int,
        onConnected: (socket: Socket) -> Unit
    ) : Server("App", port, onConnected)

    class ADB(
        port: Int = 5037,
        onConnected: (socket: Socket) -> Unit
    ) : Server("ADB", port, onConnected)

    private lateinit var mServerSocket: ServerSocket

    private val mStartLock = ReentrantLock()
    private val mStartedCondition = mStartLock.newCondition()

    private val mLock = ReentrantReadWriteLock()

    // Guarded by mLock
    var isStarted: Boolean = false
        get() = mLock.read { field }
        set(value) = mLock.write { field = value }

    /**
     * Please don't call this function directly,
     * use [tw.darkk6.adbremote.app.App.startServer] instead
     */
    internal fun internalStart(executor: ExecutorService): Boolean {
        val started = isStarted
        if (!started) {
            executor.execute(this)
            // Wait for server is real started for 3 sec
            mStartLock.withLock {
                mStartedCondition.await(3000, TimeUnit.MILLISECONDS)
            }
        }
        return isStarted
    }

    fun stop() {
        if (isStarted) {
            mServerSocket.safeClose()
        }
    }

    override fun run() {
        try {
            mServerSocket = ServerSocket(port)
            Logger.b("$serverName start, listening port $port...")

            /** Notify [internalStart] function that I am started */
            isStarted = true
            mStartLock.withLock {
                mStartedCondition.signalAll()
            }
            while (true) {
                val socket = mServerSocket.accept()
                Logger.i("===> " +
                        "Client connected to $serverName, " +
                        "${socket.remoteSocketAddress}")
                onConnected(socket)
            }
        } catch (e: Exception) {
            Logger.e("$serverName exception", e)
        }
        Logger.b("$serverName closed")
    }
}