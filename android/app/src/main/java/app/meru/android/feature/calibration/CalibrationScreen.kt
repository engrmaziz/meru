package app.meru.android.feature.calibration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.designsystem.components.MeruPrimaryButton
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruDanger
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.engine.sensors.CalibrationProfile
import app.meru.android.engine.sensors.CalibrationStore
import app.meru.android.engine.sensors.MotionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalibrationUi(
    val busy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val last: CalibrationProfile? = null,
)

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val motionEngine: MotionEngine,
    private val calibrationStore: CalibrationStore,
) : ViewModel() {
    val profile = calibrationStore.profile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _ui = MutableStateFlow(CalibrationUi())
    val ui: StateFlow<CalibrationUi> = _ui.asStateFlow()

    fun calibrate() {
        viewModelScope.launch {
            _ui.value = CalibrationUi(busy = true, message = "Hold still in your driving mount…")
            runCatching { motionEngine.calibrate(2_000) }
                .onSuccess { profile ->
                    motionEngine.stop()
                    _ui.value = CalibrationUi(
                        busy = false,
                        message = "Calibrated. Forward axis ${profile.forwardAxis}, lateral ${profile.lateralAxis}.",
                        last = profile,
                    )
                }
                .onFailure { e ->
                    motionEngine.stop()
                    _ui.value = CalibrationUi(busy = false, error = e.message ?: "Calibration failed")
                }
        }
    }

    fun clear() {
        viewModelScope.launch {
            calibrationStore.clear()
            _ui.value = CalibrationUi(message = "Cleared — recalibrate before Performance mode.")
        }
    }
}

@Composable
fun CalibrationScreen(
    onBack: () -> Unit,
    viewModel: CalibrationViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val profile by viewModel.profile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        TextButton(onClick = onBack) { Text("Back", color = MeruMuted) }
        Text("Calibrate phone", color = MeruText, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Place Meru in the mount you’ll use while driving. Keep still for two seconds.",
            color = MeruMuted,
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (profile != null) {
            Text(
                "Current: forward=${profile!!.forwardAxis} lateral=${profile!!.lateralAxis}",
                color = MeruTeal,
            )
        } else {
            Text("No calibration saved yet.", color = MeruMuted)
        }
        Spacer(modifier = Modifier.height(16.dp))
        ui.message?.let { Text(it, color = MeruTeal) }
        ui.error?.let { Text(it, color = MeruDanger) }
        Spacer(modifier = Modifier.height(20.dp))
        if (ui.busy) {
            CircularProgressIndicator(color = MeruTeal)
        } else {
            MeruPrimaryButton(text = "Start calibration", onClick = viewModel::calibrate)
        }
    }
}
