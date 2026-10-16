package xyz.xyverse.motionkraft.editor

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import xyz.xyverse.motionkraft.MotionKraftApp
import xyz.xyverse.motionkraft.MainActivity
import xyz.xyverse.motionkraft.R

class ExportService : Service() {

    companion object {
        const val NOTIF_ID = 1001
        const val ACTION_CANCEL = "xyz.xyverse.motionkraft.CANCEL_EXPORT"
    }

    private var cancelled = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            cancelled = true
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground()
        return START_STICKY
    }

    private fun startForeground() {
        val notif = buildNotification("Mempersiapkan export...", 0)
        try {
            startForeground(NOTIF_ID, notif)
        } catch (e: ForegroundServiceStartNotAllowedException) {
            // Android 12+: hanya bisa start foreground jika app di foreground
        }
    }

    fun updateProgress(progress: Float, text: String = "Exporting video") {
        val notif = buildNotification(text, (progress * 100).toInt())
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIF_ID, notif)
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, ExportService::class.java).setAction(ACTION_CANCEL)
        val cancelPIntent = PendingIntent.getService(
            this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, MotionKraftApp.CHANNEL_EXPORT)
            .setContentTitle("MotionKraft Export")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(pendingIntent)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, "Batal", cancelPIntent)
            .build()
    }
}
