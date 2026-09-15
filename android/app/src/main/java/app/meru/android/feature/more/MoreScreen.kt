package app.meru.android.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.datastore.ProgressionSnapshot
import app.meru.android.core.datastore.ProgressionStore
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.engine.sensors.CalibrationStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val calibrationStore: CalibrationStore,
    private val progressionStore: ProgressionStore,
) : ViewModel() {
    val progression = progressionStore.snapshot.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProgressionSnapshot(),
    )

    fun signOut() {
        viewModelScope.launch { sessionStore.clear() }
    }

    fun clearCalibration() {
        viewModelScope.launch { calibrationStore.clear() }
    }

    fun setNotif(levelUp: Boolean? = null, challenge: Boolean? = null, streak: Boolean? = null) {
        viewModelScope.launch { progressionStore.setNotif(levelUp, challenge, streak) }
    }
}

@Composable
fun MoreScreen(
    onOpenCalibration: () -> Unit,
    onOpenAchievements: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val progression by viewModel.progression.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("More", color = MeruText, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Cockpit settings for Meru.", color = MeruMuted)
        Spacer(modifier = Modifier.height(24.dp))
        MeruSecondaryButton(text = "Achievements", onClick = onOpenAchievements)
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(text = "Challenges", onClick = onOpenChallenges)
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(text = "Calibrate phone", onClick = onOpenCalibration)
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(
            text = "Clear calibration",
            onClick = { scope.launch { viewModel.clearCalibration() } },
        )

        Spacer(modifier = Modifier.height(28.dp))
        Text("Notifications (post-drive only)", color = MeruText, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Never mid-drive. Opt in for ascent moments.", color = MeruMuted, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))
        NotifRow("Level-up", progression.notifLevelUp) {
            viewModel.setNotif(levelUp = it)
        }
        NotifRow("Challenge complete", progression.notifChallenge) {
            viewModel.setNotif(challenge = it)
        }
        NotifRow("Streak reminders", progression.notifStreak) {
            viewModel.setNotif(streak = it)
        }

        Spacer(modifier = Modifier.height(24.dp))
        MeruSecondaryButton(
            text = "Sign out",
            onClick = { scope.launch { viewModel.signOut() } },
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun NotifRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MeruText, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedTrackColor = MeruTeal),
        )
    }
}
