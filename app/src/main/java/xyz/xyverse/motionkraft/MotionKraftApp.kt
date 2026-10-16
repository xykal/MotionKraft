package xyz.xyverse.motionkraft

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MotionKraftApp : Application() {

    data class PendingImport(
        val path: String,
        val isVideo: Boolean,
        val durationUs: Long
    )

    @Volatile
    var pendingImport: PendingImport? = null

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
            ).apply { description = "Video export progress" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
