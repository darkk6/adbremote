package tw.darkk6.adbremote.data

import tw.darkk6.adbremote.app.Constants
import java.io.File


data class Config(
    var role: String = "server",
    var log: Log = Log(),
    var server: Server = Server(),
    var client: Client = Client(),
    var adb: Adb = Adb()
) {

    fun update(
        argRole: String?, argLogLevel: String?, argHost: String?, argPort: String?,
        argAdbRole: String?, argAdbPort: String?, argAdbPath: String?, argAdbSockets: String?
    ) {
        argRole?.also { role = it }
        argLogLevel?.also { log.level = it }
        argHost?.also { client.host = it }
        argPort?.toIntOrNull()?.also { port ->
            server.port = port
            client.port = port
        }

        argAdbRole?.also { adb.role = it }
        argAdbPort?.toIntOrNull()?.also { adb.port = it }
        argAdbPath?.also { adb.folder = it }
        argAdbSockets?.toIntOrNull()?.also { adb.sockets = it }
    }

    fun roleType(): Role {
        return if (role == "server") Role.SERVER else Role.CLIENT
    }

    enum class Role { SERVER, CLIENT }

    enum class LogLevel { NONE, BASIC, INFO, ERROR, DEBUG }

    data class Log(var level: String = "BASIC") {
        fun getLogLevel(): LogLevel {
            return try {
                LogLevel.valueOf(level.toUpperCase())
            } catch (ignore: Exception) {
                LogLevel.INFO
            }
        }
    }

    data class Server(var port: Int = 9999)

    data class Client(var host: String = "127.0.0.1", var port: Int = 9999)

    data class Adb(
        var role: String = "client",
        var port: Int = Constants.ADB_DEFAULT_PORT,
        var folder: String = "./platform-tool",
        var sockets: Int = 5
    ) {
        fun roleType(): Role {
            return if (role == "server") Role.SERVER else Role.CLIENT
        }

        fun adbPath(forceWindows: Boolean = false): String {
            val adbFileName = if (forceWindows) "adb.exe" else "adb"
            return File(folder, adbFileName).absolutePath
        }

        fun adbExists(): Boolean {
            val file = File(adbPath())
            val fileWindows = File(adbPath(true))
            return file.isFile || fileWindows.isFile
        }
    }
}