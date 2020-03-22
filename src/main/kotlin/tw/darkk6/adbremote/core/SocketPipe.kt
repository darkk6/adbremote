package tw.darkk6.adbremote.core

import tw.darkk6.adbremote.app.App
import tw.darkk6.adbremote.core.ktx.safeClose
import tw.darkk6.adbremote.util.Logger
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService

class SocketPipe(
    private val socketFrom: Socket,
    private val socketTo: Socket,
    private val onFinish: (SocketPipe.() -> Unit)? = null,
    private val executor: ExecutorService = App.executor
) : Runnable, Closeable {

    companion object {
        private const val BUFF_SIZE = 1024
    }

    private val singleQueue = ArrayBlockingQueue<Boolean>(1)
    private val timeoutTimer = Timer()

    private fun pipe(ipt: InputStream, opt: OutputStream): Runnable {
        return Runnable {
            val theBuffer = ByteArray(BUFF_SIZE)
            try {
                while (true) {
                    val theBytesRead = ipt.read(theBuffer)
                    if (theBytesRead == -1) break
                    opt.write(theBuffer, 0, theBytesRead)
                    opt.flush()
                }
            } catch (e: Throwable) {
            }
            try {
                singleQueue.put(true)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    override fun run() {
        try {
            executor.execute(pipe(socketFrom.getInputStream(), socketTo.getOutputStream()))

            executor.execute(pipe(socketTo.getInputStream(), socketFrom.getOutputStream()))

            singleQueue.take()

        } catch (e: Throwable) {
            Logger.e("Socket pipe error", e)
        } finally {
            close()
        }
    }

    override fun close() {
        socketFrom.safeClose()
        socketTo.safeClose()
        onFinish?.invoke(this)
    }
}