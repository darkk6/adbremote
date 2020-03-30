package tw.darkk6.adbremote.app

object Constants {

    object Build {
        const val NAME = "ADB Remote"
        const val MAJOR = 1
        const val MINOR = 0
        const val BUILD = 0
        const val CODE = 0
        fun getFullVersion(): String {
            return "$MAJOR.$MINOR.$BUILD-$CODE"
        }
    }

    const val CONNECT_TIMEOUT_MS = 5000
    const val ADB_DEFAULT_PORT = 5037
    const val LOCAL_HOST = "127.0.0.1"
}