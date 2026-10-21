package xyz.xyverse.motionkraft.editor

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import xyz.xyverse.motionkraft.MainActivity
import xyz.xyverse.motionkraft.MotionKraftApp
import xyz.xyverse.motionkraft.R
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

data class ExportState(
    val running: Boolean = false,
    val progress: Float = 0f,
    val resultName: String? = null,
    val error: String? = null
)

/**
 * Runs the FFmpeg export as a foreground service so it survives the app going to the
 * background. Progress is published through [state]; the saved file is added to the
 * gallery under Movies/MotionKraft.
 */
class ExportService : Service() {

    companion object {
        private const val NOTIF_ID = 1001
        private const val EXTRA_ARGS = "args"
        private const val EXTRA_OUTPUT = "output"
        private const val EXTRA_NAME = "name"
        private const val EXTRA_TOTAL_US = "total_us"
        const val ACTION_CANCEL = "xyz.xyverse.motionkraft.CANCEL_EXPORT"

        private val _state = MutableStateFlow(ExportState())
        val state: StateFlow<ExportState> = _state

        fun start(context: Context, args: List<String>, outputPath: String, displayName: String, totalUs: Long) {
            val intent = Intent(context, ExportService::class.java)
                .putStringArrayListExtra(EXTRA_ARGS, ArrayList(args))
                .putExtra(EXTRA_OUTPUT, outputPath)
                .putExtra(EXTRA_NAME, displayName)
                .putExtra(EXTRA_TOTAL_US, totalUs)
            ContextCompat.startForegroundService(context, intent)
        }

        fun acknowledge() {
            _state.value = _state.value.copy(resultName = null, error = null)
        }
    }

    private val sessionRef = AtomicReference<FFmpegSession?>(null)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val request = intent ?: return START_NOT_STICKY
        if (request.action == ACTION_CANCEL) {
            sessionRef.get()?.cancel()
            return START_NOT_STICKY
        }
        val args = request.getStringArrayListExtra(EXTRA_ARGS)
        val output = request.getStringExtra(EXTRA_OUTPUT)
        val name = request.getStringExtra(EXTRA_NAME)
        if (args.isNullOrEmpty() || output == null || name == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val totalMs = (request.getLongExtra(EXTRA_TOTAL_US, 0L) / 1000L).coerceAtLeast(1L)

        ServiceCompat.startForeground(
            this, NOTIF_ID, buildNotification(getString(R.string.export_preparing), 0),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        _state.value = ExportState(running = true, progress = 0f)
        scope.launch { runExport(ArrayList(args), output, name, totalMs) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun runExport(args: List<String>, output: String, name: String, totalMs: Long) {
        val result: ReturnCode? = suspendCancellableCoroutine { cont ->
            val session = FFmpegKit.executeWithArgumentsAsync(
                args.toTypedArray(),
                { finished -> cont.resume(finished.returnCode, null) },
                { _ -> },
                { stats ->
                    val p = (stats.time.toFloat() / totalMs).coerceIn(0f, 1f)
                    _state.value = _state.value.copy(progress = p)
                    updateNotification(getString(R.string.export_running), (p * 100).toInt())
                }
            )
            sessionRef.set(session)
            cont.invokeOnCancellation { session.cancel() }
        }
        sessionRef.set(null)

        val success = result != null && ReturnCode.isSuccess(result)
        val cancelled = result != null && ReturnCode.isCancel(result)
        if (success) {
            val shown = try {
                saveToGallery(File(output), name)
            } catch (e: Exception) {
                null
            }
            _state.value = ExportState(
                running = false,
                progress = 1f,
                resultName = shown,
                error = if (shown == null) getString(R.string.export_save_failed) else null
            )
        } else {
            _state.value = ExportState(
                running = false,
                error = getString(if (cancelled) R.string.export_cancelled else R.string.export_failed)
            )
        }
        File(output).delete()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** Adds the finished file to the gallery. Returns a human-readable location. */
    private fun saveToGallery(src: File, name: String): String {
        val resolver = contentResolver
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/MotionKraft")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("MediaStore insert failed")
            val out = resolver.openOutputStream(uri) ?: throw IOException("Cannot open output")
            out.use { stream -> src.inputStream().use { it.copyTo(stream) } }
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return "Movies/MotionKraft/$name"
        }
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "MotionKraft")
        dir.mkdirs()
        val target = File(dir, name)
        src.copyTo(target, overwrite = true)
        MediaScannerConnection.scanFile(this, arrayOf(target.absolutePath), arrayOf("video/mp4"), null)
        return target.absolutePath
    }

    private fun updateNotification(text: String, progress: Int) {
        getSystemService(NotificationManager::class.java).notify(NOTIF_ID, buildNotification(text, progress))
    }

    private fun buildNotification(text: String, progress: Int): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancel = PendingIntent.getService(
            this, 1, Intent(this, ExportService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MotionKraftApp.CHANNEL_EXPORT)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentIntent(open)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_delete, getString(R.string.cancel), cancel)
            .build()
    }
}
