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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.datastore.ProgressionSnapshot
import app.meru.android.core.datastore.ProgressionStore
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruAmber
import app.meru.android.core.designsystem.theme.MeruCyan
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.core.flags.FeatureFlags
import app.meru.android.core.network.MeruApi
import app.meru.android.core.network.PrivacyPatchRequest
import app.meru.android.engine.sensors.CalibrationStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class MoreViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val calibrationStore: CalibrationStore,
    private val progressionStore: ProgressionStore,
    private val api: MeruApi,
    private val featureFlags: FeatureFlags,
) : ViewModel() {
    val progression = progressionStore.snapshot.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProgressionSnapshot(),
    )

    val flags = featureFlags.flags

    private val _boardOptIn = MutableStateFlow(true)
    val boardOptIn: StateFlow<Boolean> = _boardOptIn.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        viewModelScope.launch { featureFlags.refresh() }
    }

    fun refreshPrivacy() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                _boardOptIn.value = api.privacyMe("Bearer $token").boardOptIn
            }
        }
    }

    fun setBoardOptIn(optIn: Boolean) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                _boardOptIn.value = api.privacyPatch(
                    "Bearer $token",
                    PrivacyPatchRequest(boardOptIn = optIn),
                ).boardOptIn
            }
        }
    }

    fun signOut() {
        viewModelScope.launch { sessionStore.clear() }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                api.deleteAccount("Bearer $token")
                sessionStore.clear()
            }.onFailure {
                _message.value = "Delete failed — try again"
            }
        }
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
    onOpenLeaderboards: () -> Unit = {},
    onOpenAdventureMap: () -> Unit = {},
    onOpenWorkshops: () -> Unit = {},
    onOpenBookings: () -> Unit = {},
    onOpenStamp: () -> Unit = {},
    driving: Boolean = false,
    viewModel: MoreViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val progression by viewModel.progression.collectAsState()
    val boardOptIn by viewModel.boardOptIn.collectAsState()
    val flags by viewModel.flags.collectAsState()
    val message by viewModel.message.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshPrivacy() }

    val bayOpen = flags.s4Marketplace && flags.bookingsEnabled
    val arenaOpen = flags.s2Leaderboards

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
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Soft launch · ${flags.softLaunchCityName}",
            color = MeruCyan,
            fontSize = 13.sp,
        )
        if (driving) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Driving Mode — Arena & Bay locked", color = MeruAmber, fontSize = 13.sp)
        }
        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MeruAmber, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
        MeruSecondaryButton(
            text = when {
                driving -> "Bay (locked)"
                !bayOpen -> "Bay (paused)"
                else -> "Bay — find workshops"
            },
            onClick = { if (!driving && bayOpen) onOpenWorkshops() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(
            text = if (driving || !bayOpen) "Bookings (locked)" else "My bookings",
            onClick = { if (!driving && bayOpen) onOpenBookings() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(
            text = if (driving || !bayOpen) "Stamp (locked)" else "Stamp — invoices & jobs",
            onClick = { if (!driving && bayOpen) onOpenStamp() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(
            text = when {
                driving -> "Arena (locked)"
                !arenaOpen -> "Arena (paused)"
                else -> "Arena boards"
            },
            onClick = { if (!driving && arenaOpen) onOpenLeaderboards() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(
            text = if (driving || !arenaOpen) "Adventure map (locked)" else "Adventure map",
            onClick = { if (!driving && arenaOpen) onOpenAdventureMap() },
        )
        Spacer(modifier = Modifier.height(12.dp))
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
        Text("Leaderboard privacy", color = MeruText, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        NotifRow("Show me on boards", boardOptIn) { viewModel.setBoardOptIn(it) }

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
        if (flags.accountDeletionEnabled) {
            Spacer(modifier = Modifier.height(12.dp))
            MeruSecondaryButton(
                text = "Delete account",
                onClick = { viewModel.deleteAccount() },
            )
            Text(
                "Purges vault data from Meru servers (Play Data Safety).",
                color = MeruMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
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
