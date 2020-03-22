package tw.darkk6.adbremote.app

import io.reactivex.rxjava3.disposables.Disposable
import tw.darkk6.adbremote.core.Server
import tw.darkk6.adbremote.data.Config
import tw.darkk6.adbremote.util.AdbUtil
import tw.darkk6.adbremote.util.Logger
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


object App {

    val executor: ExecutorService = Executors.newCachedThreadPool()

    private val mAdbServerScheduler = Executors.newScheduledThreadPool(1);

    private var mDisposable: Disposable? = null

    lateinit var config: Config
        private set

    private val mServerList = ArrayList<Server>()

    val isInited: Boolean
        get() = this::config.isInitialized

    fun init(config: Config) {
        App.config = config
        Runtime.getRuntime().addShutdownHook(object : Thread() {
            override fun run() {
                shutdown()
            }
        })
    }

    fun run() {
        AdbUtil.init(config.adb.adbPath())

        when (config.adb.roleType()) {
            Config.Role.SERVER -> AdbUtil.killServer()
            Config.Role.CLIENT -> startAdbServerPeriodically()
        }

        mDisposable = BehaviorHandler.handle(config)
    }

    fun startServer(server: Server) {
        server.internalStart(executor)
        registerServer(server)
    }

    private fun startAdbServerPeriodically() {
        mAdbServerScheduler.scheduleAtFixedRate({
            try {
                AdbUtil.startServer()
            } catch (e: IOException) {
                Logger.e("Unable to start adb server", e)
            }
        }, 0, 30, TimeUnit.SECONDS)
    }

    private fun registerServer(vararg servers: Server) {
        mServerList.addAll(servers)
    }

    private fun shutdown() {
        mServerList.forEach { it.stop() }
        mAdbServerScheduler.shutdownNow()
        mDisposable?.dispose()
    }
}