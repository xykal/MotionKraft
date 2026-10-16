package xyz.xyverse.motionkraft

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xyverse.motionkraft.presentation.EditorViewModel
import xyz.xyverse.motionkraft.presentation.MotionKraftRoot
import xyz.xyverse.motionkraft.ui.theme.MotionKraftTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private val requiredPermissions = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_MEDIA_VIDEO)
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { importMedia(it) }
    }

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        ensurePermissions()

        setContent {
            MotionKraftTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val vm: EditorViewModel = viewModel()
                    MotionKraftRoot(
                        onPickMedia = { pickMedia.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.VideoOnly
                            )
                        ) },
                        onPickImage = { pickMedia.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        ) },
                        vm = vm
                    )
                }
            }
        }
    }

    private fun ensurePermissions() {
        val missing = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permLauncher.launch(missing.toTypedArray())
        }
    }

    private fun importMedia(uri: Uri) {
        val vm: EditorViewModel? = null // handled via viewModel in compose, but we can dispatch here
        // Copy to app cache + detect type + duration via MediaMetadataRetriever
        val app = application as MotionKraftApp
        lifecycle.addObserver(object : androidx.lifecycle.DefaultLifecycleObserver {
            // Will resolve via viewmodel accessor below
        })
        // We send via broadcast-like approach: write to a known temp file and signal
        Thread {
            try {
                val resolver = contentResolver
                val mimeType = resolver.getType(uri) ?: ""
                val isImage = mimeType.startsWith("image/")
                val isVideo = mimeType.startsWith("video/")

                // Copy to app files dir
                val ext = when {
                    isImage -> "jpg"
                    isVideo -> "mp4"
                    else -> "bin"
                }
                val dest = File(cacheDir, "import_${System.currentTimeMillis()}.$ext")
                resolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { output -> input.copyTo(output) }
                }

                var durationUs = 3_000_000L
                if (isVideo) {
                    val mmr = android.media.MediaMetadataRetriever()
                    mmr.setDataSource(dest.absolutePath)
                    val dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                    mmr.release()
                    durationUs = (dur?.toLongOrNull() ?: 3000L) * 1000L
                }

                // Stash import result in app holder, VM will pick it up on next compose frame
                app.pendingImport = MotionKraftApp.PendingImport(
                    path = dest.absolutePath,
                    isVideo = isVideo,
                    durationUs = durationUs
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }
}
