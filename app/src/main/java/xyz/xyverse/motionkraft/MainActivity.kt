package xyz.xyverse.motionkraft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import xyz.xyverse.motionkraft.presentation.MotionKraftApp
import xyz.xyverse.motionkraft.ui.theme.MotionKraftTheme

class MainActivity : ComponentActivity() {

    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // Handle media pick, dihubungkan ke viewModel
        uri?.let {
            // viewModel.importMedia(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MotionKraftTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MotionKraftApp()
                }
            }
        }
    }
}
