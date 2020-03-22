package tw.darkk6.adbremote.core.ktx

import java.io.Closeable

fun Closeable.safeClose() {
    try {
        close()
    } catch (ignore: Exception) {
    }
}
