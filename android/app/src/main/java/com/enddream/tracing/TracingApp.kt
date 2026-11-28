package com.enddream.tracing

import android.app.Application
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class TracingApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val text = runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                sw.toString()
            }.getOrDefault("")
            runCatching {
                File(filesDir, "crash.txt").writeText(text)
            }
            if (touchesAmap(text) || AmapLoc.wasInflight(applicationContext)) {
                runCatching { AmapLoc.markBroken(applicationContext) }
            }
            prev?.uncaughtException(thread, throwable)
        }
    }

    private fun touchesAmap(text: String): Boolean =
        text.contains("com.amap.") || text.contains("com.loc.") || text.contains("com.autonavi.")

    companion object {
        fun lastCrash(app: Application): String? = runCatching {
            val f = File(app.filesDir, "crash.txt")
            if (f.exists()) f.readText() else null
        }.getOrNull()

        fun clearCrash(app: Application) {
            runCatching { File(app.filesDir, "crash.txt").delete() }
        }
    }
}
