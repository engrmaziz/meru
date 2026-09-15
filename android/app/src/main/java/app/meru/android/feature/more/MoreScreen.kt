package app.meru.android.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.engine.sensors.CalibrationStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val calibrationStore: CalibrationStore,
) : ViewModel() {
    fun signOut() {
        viewModelScope.launch { sessionStore.clear() }
    }

    fun clearCalibration() {
        viewModelScope.launch { calibrationStore.clear() }
    }
}

@Composable
fun MoreScreen(
    onOpenCalibration: () -> Unit,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text("More", color = MeruText, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Cockpit settings for Meru.", color = MeruMuted)
        Spacer(modifier = Modifier.height(24.dp))
        MeruSecondaryButton(text = "Calibrate phone", onClick = onOpenCalibration)
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(
            text = "Clear calibration",
            onClick = { scope.launch { viewModel.clearCalibration() } },
        )
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(
            text = "Sign out",
            onClick = { scope.launch { viewModel.signOut() } },
        )
    }
}
