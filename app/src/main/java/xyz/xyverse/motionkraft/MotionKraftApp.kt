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
                getString(R.string.export_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
