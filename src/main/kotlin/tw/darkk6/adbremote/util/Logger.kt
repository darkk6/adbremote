package tw.darkk6.adbremote.util

import tw.darkk6.adbremote.app.App
import tw.darkk6.adbremote.data.Config

object Logger {

    fun b(message: String) {
        val level = getLogLevel()
        if (level >= Config.LogLevel.BASIC) {
            println("[BASIC] $message")
        }
    }

    fun i(message: String) {
        val level = getLogLevel()
        if (level >= Config.LogLevel.INFO) {
            println("[INFO ] $message")
        }
    }

    fun d(message: String) {
        val level = getLogLevel()
        if (level >= Config.LogLevel.DEBUG) {
            println("[DEBUG] $message")
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        val level = getLogLevel()
        if (level >= Config.LogLevel.ERROR) {
            System.err.println("[ERROR] $message")
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
}