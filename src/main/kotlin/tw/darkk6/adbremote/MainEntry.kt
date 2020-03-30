package tw.darkk6.adbremote

import org.yaml.snakeyaml.Yaml
import tw.darkk6.adbremote.app.App
import tw.darkk6.adbremote.app.Constants
import tw.darkk6.adbremote.data.Config
import tw.darkk6.adbremote.util.ArgumentParser
import tw.darkk6.adbremote.util.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


object MainEntry {

    @JvmStatic
    fun main(vararg args: String) {
        println("\n${Constants.Build.NAME} v${Constants.Build.getFullVersion()}")

        val argParser = ArgumentParser(*args)
        if (argParser.hasParameter("h")) {
            showUsage()
            return
        }
        val config = initConfig(argParser) ?: return
        App.init(config)

        if (!config.adb.adbExists()) {
            Logger.e("adb binary not found : ${config.adb.adbPath()}")
            return
        }

        App.run()
    }

    private fun initConfig(parser: ArgumentParser): Config? {
        var configPath = parser.getParameter("config")

        if (configPath == null) {
            createDefaultConfig()
            configPath = "./config.yaml"
        }

        val configFile = File(configPath)
        if (!configFile.isFile) {
            Logger.e("Config file not found : $configPath")
            return null
        } else if (!configFile.canRead()) {
            Logger.e("Config file can NOT be read : $configPath")
            return null
        }
        val yaml = Yaml()
        val config = yaml.loadAs(FileInputStream(configFile), Config::class.java)

        // override parameters for general, server and client
        val argRole = parser.getParameter("role")
        val argLogLevel = parser.getParameter("log")
        val argHost = parser.getParameter("host")
        val argPort = parser.getParameter("port")

        // override parameters for adb
        val argAdbRole = parser.getParameter("adbrole")
        val argAdbPort = parser.getParameter("adbport")
        val argAdbPath = parser.getParameter("adbfolder")
        val argAdbSockets = parser.getParameter("sockets")
        config.update(
            argRole, argLogLevel, argHost, argPort,
            argAdbRole, argAdbPort, argAdbPath, argAdbSockets
        )

        return config
    }

    private fun createDefaultConfig() {
        val dest = File("config.yaml")
        if (dest.isFile) {
            Logger.i("config file is already exists, skip create default config file")
            return
        }
        println(dest)
        val ipt = javaClass.classLoader.getResourceAsStream("resources/defaultConfig.yaml") ?: return
        val opt = FileOutputStream(dest)
        opt.use { output ->
            ipt.use { input ->
                input.copyTo(output)
            }
        }
        Logger.b("Default config created to config.yaml")
    }

    private fun showUsage() {
        println(
            """ |Usage:
                |   java -jar ADBRemote.jar [parameters]
                |
                |parameters:
                |   -h:
                |       show this help
                |       
                |   -config path/to/config:
                |       config file path
                |   
                |   -log NONE/BASIC/INFO/DEBUG/ERROR:
                |       log level, default is BASIC
                |       
                |   -role client/server:
                |       Set application role
                |   
                |   -host xx.xx.xx.xx:
                |       for application client mode, the server's host
                |       
                |   -port n:
                |       In application client mode, the server's port
                |       In application server mode, the listening port
                |       
                |   -adbrole server/client:
                |       set adb mode
                |       
                |   -adbport m:
                |       the adb port, default is 5037
                |
                |   -adbfolder path/to/adb:
                |       adb binary folder
                |       
                |   -sockets num:
                |       Only valid for client-client mode.
                |       The max number for keep-alive client sockets.
            """.trimMargin("|")
        )
    }
}