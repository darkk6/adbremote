package tw.darkk6.adbremote.util

import tw.darkk6.adbremote.app.App
import tw.darkk6.adbremote.data.Config
import java.text.SimpleDateFormat
import java.util.*


object Logger {

    private val formatter = SimpleDateFormat("HH:mm:ss.SSS")

    fun b(message: String) {
        val level = getLogLevel()
        if (level >= Config.LogLevel.BASIC) {
            stdout(Config.LogLevel.BASIC, message)
        }
    }

    fun i(message: String) {
        val level = getLogLevel()
        if (level >= Config.LogLevel.INFO) {
            stdout(Config.LogLevel.INFO, message)
        }
    }

    fun d(message: String) {
        val level = getLogLevel()
        if (level >= Config.LogLevel.DEBUG) {
            stdout(Config.LogLevel.DEBUG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        val level = getLogLevel()
        if (level >= Config.LogLevel.ERROR) {
            stdout(Config.LogLevel.ERROR, message)
            throwable?.printStackTrace()
        }
    }

    private fun getLogLevel(): Config.LogLevel {
        return if (App.isInited) {
            App.config.log.getLogLevel()
        } else {
            Config.LogLevel.DEBUG
        }
    }

    private fun stdout(level: Config.LogLevel, message: String) {
        val out = if (level == Config.LogLevel.ERROR) {
            System.err
        } else {
            System.out
        }
        out.printf("%s [%-5s] %s\n", formatter.format(Date()), level.name, message)
    }
}