package app.meru.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier.Modifier
import app.meru.android.core.designsystem.theme.MeruTheme
import app.meru.android.ui.MeruRoot
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeruTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MeruRoot()
                }
            }
        }
    }
}
