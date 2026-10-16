package xyz.xyverse.motionkraft

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MotionKraftApp : Application() {
    companion object {
        const val CHANNEL_EXPORT = "export_channel"
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_EXPORT,
                "Export Video",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifikasi saat mengexport video"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }
}
