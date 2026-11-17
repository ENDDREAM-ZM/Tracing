package com.enddream.tracing

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

object Notify {

    private const val CHANNEL_ID = "xingji_arrival"
    private const val CHANNEL_NAME = "到站提醒"

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (mgr.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                )
                ch.enableVibration(true)
                ch.vibrationPattern = longArrayOf(0, 400, 200, 400)
                mgr.createNotificationChannel(ch)
            }
        }
    }

    fun canNotify(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    fun fire(ctx: Context, id: Int, title: String, text: String) {
        if (!canNotify(ctx)) {
            vibrate(ctx)
            return
        }
        ensureChannel(ctx)
        runCatching {
            val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .build()
            val mgr = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.notify(id, n)
        }
        vibrate(ctx)
    }

    fun vibrator(ctx: Context): Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }.getOrNull()

    fun vibrate(ctx: Context) {
        runCatching {
            val v = vibrator(ctx) ?: return
            if (!v.hasVibrator()) return
            val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
            Thread {
                runCatching {
                    Thread.sleep(6000L)
                    v.cancel()
                }
            }.start()
        }
    }

    fun stop(ctx: Context) {
        runCatching { vibrator(ctx)?.cancel() }
    }
}
