package tw.darkk6.adbremote.app

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.BiFunction
import io.reactivex.rxjava3.subjects.BehaviorSubject
import io.reactivex.rxjava3.subjects.PublishSubject
import tw.darkk6.adbremote.core.Client
import tw.darkk6.adbremote.core.Server
import tw.darkk6.adbremote.core.SocketPipe
import tw.darkk6.adbremote.data.Config
import tw.darkk6.adbremote.util.Logger
import java.net.Socket
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object BehaviorHandler {

    private lateinit var config: Config

    fun handle(config: Config): Disposable? {
        this.config = config
        val app = config.roleType()
        val adb = config.adb.roleType()
        return when {
            app == Config.Role.SERVER && adb == Config.Role.SERVER -> {
                Logger.b(
                    """
                    |
                    |=====================================================
                    | Run with Server[${config.server.port}] - Server[${config.adb.port}] mode
                    |=====================================================""".trimMargin("|")
                )
                handleAppServerAdbServer()
            }
            app == Config.Role.SERVER && adb == Config.Role.CLIENT -> {
                Logger.b(
                    """
                    |
                    |=====================================================
                    | Run with Server[${config.server.port}] - Client[${config.adb.port}] mode
                    |=====================================================""".trimMargin("|")
                )
                handleAppServerAdbClient()
            }
            app == Config.Role.CLIENT && adb == Config.Role.SERVER -> {
                Logger.b(
                    """
                    |
                    |=====================================================
                    | Run with Client[${config.client.host}:${config.client.port}] - Server[${config.adb.port}] mode
                    |=====================================================""".trimMargin("|")
                )
                handleAppClientAdbServer()
            }
            app == Config.Role.CLIENT && adb == Config.Role.CLIENT -> {
                Logger.b(
                    """
                    |
                    |=====================================================
                    | Run with Client[${config.client.host}:${config.client.port}] - Client[${config.adb.port}] mode
                    |=====================================================""".trimMargin("|")
                )
                handleAppClientAdbClient()
                null
            }
            else -> throw IllegalStateException("This should never happen")
        }
    }

    private fun handleAppServerAdbServer(): Disposable {
        val appSubject: PublishSubject<Socket> = PublishSubject.create()
        val adbSubject: PublishSubject<Socket> = PublishSubject.create()
        App.startServer(Server.App(config.server.port, appSubject::onNext))
        App.startServer(Server.ADB(config.adb.port, adbSubject::onNext))

        return Observable.zip(adbSubject, appSubject, BiFunction<Socket, Socket, SocketPipe> { adbClient, appClient ->
            SocketPipe(adbClient, appClient)
        }).subscribe { socketPipe ->
            App.executor.execute(socketPipe)
        }
    }

    private fun handleAppServerAdbClient(): Disposable {
        val appSubject = PublishSubject.create<Socket>()
        App.startServer(Server.App(config.server.port, appSubject::onNext))
        return appSubject.subscribe { appSocket ->
            Client.ADB(config.adb.port).connect { adbSocket ->
                App.executor.execute(SocketPipe(appSocket, adbSocket))
            }
        }
    }

    private fun handleAppClientAdbServer(): Disposable {
        val adbSubject = PublishSubject.create<Socket>()
        App.startServer(Server.ADB(config.adb.port, adbSubject::onNext))
        return adbSubject.subscribe { adbSocket ->
            Client.App(config.client.host, config.client.port).connect { appSocket ->
                App.executor.execute(SocketPipe(adbSocket, appSocket))
            }
        }
    }

    private val mLock = ReentrantLock()

    // Guarded by mLock
    private val mSocketSet = HashSet<SocketPipe>()
    private var mClientToClientDisposable: Disposable? = null
    private fun handleAppClientAdbClient() {
        val adbSubject: BehaviorSubject<Socket> = BehaviorSubject.create()
        val appSubject: BehaviorSubject<Socket> = BehaviorSubject.create()
        var appOk = false

        while (!appOk) {
            appOk = Client.App(config.client.host, config.client.port).connect(appSubject::onNext)
            if (!appOk) {
                Logger.b("Connect to ${config.client.host}:${config.client.port} fail, retry in 5 sec...")
                Thread.sleep(5000L)
            }
        }
        Logger.b("App connected to server")

        var adbOk = false
        while (!adbOk) {
            adbOk = Client.ADB(config.adb.port).connect(adbSubject::onNext)
            if (!adbOk) {
                Logger.b("Connect to ADB fail, retry in 5 sec...")
                Thread.sleep(5000L)
            }
        }
        Logger.b("Connected to ADB success")

        mClientToClientDisposable?.dispose()
        mClientToClientDisposable =
            Observable.zip(appSubject, adbSubject, BiFunction<Socket, Socket, SocketPipe> { appSocket, adbSocket ->
                SocketPipe(appSocket, adbSocket, onFinish = {
                    mLock.withLock {
                        mSocketSet.remove(this)
                        Logger.i("Current connections: ${mSocketSet.size} / ${config.adb.sockets}")
                        if (mSocketSet.size < config.adb.sockets) {
                            handleAppClientAdbClient()
                        }
                    }
                })
            }).subscribe { socketPipe ->
                App.executor.execute(socketPipe)
                mLock.withLock {
                    mSocketSet.add(socketPipe)
                    Logger.i("Current connections: ${mSocketSet.size} / ${config.adb.sockets}")
                    if (mSocketSet.size < config.adb.sockets) {
                        handleAppClientAdbClient()
                    }
                }
            }
    }
}