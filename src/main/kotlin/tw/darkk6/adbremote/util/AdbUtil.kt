package tw.darkk6.adbremote.util

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


object AdbUtil {

    private lateinit var adbPath: String

    fun init(adbPath: String) {
        this.adbPath = adbPath
    }

    @Throws(IOException::class)
    fun listDevices(): String? {
        return executeAdbCommand("devices")
    }

    @Throws(IOException::class)
    fun killServer(): String? {
        return executeAdbCommand("kill-server")
    }

    @Throws(IOException::class)
    fun startServer(): String? {
        return executeAdbCommand("start-server")
    }

    @Throws(IOException::class)
    private fun executeAdbCommand(aCommand: String): String? {
        val cmd = "$adbPath $aCommand"
        Logger.d("Executing '$cmd'")
        val theProcess = Runtime.getRuntime().exec(cmd)
        val theReader = BufferedReader(InputStreamReader(theProcess.inputStream))
        val theBuilder = StringBuilder()
        var theLine: String?
        while (theReader.readLine().also { theLine = it } != null) {
            theBuilder.append(theLine)
        }
        return theBuilder.toString()
    }
}