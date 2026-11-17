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
            runCatching {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                File(filesDir, "crash.txt").writeText(sw.toString())
            }
            prev?.uncaughtException(thread, throwable)
        }
    }

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
