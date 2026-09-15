package app.meru.android.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.database.ExplorationDao
import app.meru.android.core.database.TripDao
import app.meru.android.core.datastore.ProgressionSnapshot
import app.meru.android.core.datastore.ProgressionStore
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.theme.MeruAmber
import app.meru.android.core.designsystem.theme.MeruCyan
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.core.network.ChallengeItemDto
import app.meru.android.core.network.MeruApi
import app.meru.android.core.network.RankChipDto
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeLocalStats(
    val tripCount: Int = 0,
    val distanceKm: Double = 0.0,
    val cells: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val tripDao: TripDao,
    private val explorationDao: ExplorationDao,
    private val progressionStore: ProgressionStore,
    private val api: MeruApi,
) : ViewModel() {
    val session = sessionStore.session.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    val progression = progressionStore.snapshot.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProgressionSnapshot(),
    )

    private val _local = MutableStateFlow(HomeLocalStats())
    val local: StateFlow<HomeLocalStats> = _local.asStateFlow()

    private val _challenge = MutableStateFlow<ChallengeItemDto?>(null)
    val challenge: StateFlow<ChallengeItemDto?> = _challenge.asStateFlow()

    private val _ranks = MutableStateFlow<List<RankChipDto>>(emptyList())
    val ranks: StateFlow<List<RankChipDto>> = _ranks.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init {
        viewModelScope.launch {
            tripDao.observeCompletedTrips().collect { trips ->
                _local.value = HomeLocalStats(
                    tripCount = trips.size,
                    distanceKm = trips.sumOf { it.distanceM } / 1000.0,
                    cells = explorationDao.cellCount(),
                )
            }
        }
        refreshServer()
    }

    fun refreshServer() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken
            if (token.isNullOrBlank()) return@launch
            runCatching {
                val me = api.scoresMe("Bearer $token")
                progressionStore.applyScores(me)
                val challenges = api.challenges("Bearer $token").items
                _challenge.value = challenges.firstOrNull { !it.completed } ?: challenges.firstOrNull()
                _ranks.value = api.ranksMe("Bearer $token").ranks
                _refreshError.value = null
            }.onFailure {
                _refreshError.value = "Offline — showing cached ascent"
            }
        }
    }
}

@Composable
fun HomeScreen(
    onOpenAchievements: () -> Unit = {},
    onOpenChallenges: () -> Unit = {},
    onOpenLeaderboards: () -> Unit = {},
    onOpenAdventureMap: () -> Unit = {},
    driving: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsState()
    val progression by viewModel.progression.collectAsState()
    val local by viewModel.local.collectAsState()
    val challenge by viewModel.challenge.collectAsState()
    val ranks by viewModel.ranks.collectAsState()
    val refreshError by viewModel.refreshError.collectAsState()
    val name = session?.displayName ?: "Driver"
    val a11y = LocalAccessibilityManager.current
    val reduceMotion = a11y?.isEnabled == true

    val xpProgress = if (progression.xpForNextLevel <= 0) 0f
    else progression.xpIntoLevel / progression.xpForNextLevel.toFloat()
    val animatedXp by animateFloatAsState(
        targetValue = xpProgress,
        animationSpec = tween(if (reduceMotion) 0 else 900),
        label = "xpBar",
    )
    val streakScale = remember { Animatable(1f) }
    LaunchedEffect(progression.streakDays) {
        if (!reduceMotion && progression.streakDays > 0) {
            streakScale.snapTo(0.85f)
            streakScale.animateTo(1f, tween(500))
        }
    }
    LaunchedEffect(Unit) { viewModel.refreshServer() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Good to see you,", color = MeruMuted, fontSize = 14.sp)
        Text(name, color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text(progression.title, color = MeruTeal, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        refreshError?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MeruAmber, fontSize = 12.sp)
        }

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MeruElevated)
                .padding(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("LEVEL ${progression.level}", color = MeruTeal, fontWeight = FontWeight.Bold)
                Text(
                    "${progression.xpIntoLevel} / ${progression.xpForNextLevel} XP",
                    color = MeruMuted,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { animatedXp },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = MeruTeal,
                trackColor = MeruVoid,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Server XP · weights v${progression.weightsVersion.coerceAtLeast(1)}",
                color = MeruMuted,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScoreChip(
                modifier = Modifier.weight(1f),
                label = "Adventure",
                value = progression.adventureScore.toString(),
                accent = MeruCyan,
            )
            ScoreChip(
                modifier = Modifier.weight(1f),
                label = "Driver rating",
                value = progression.driverRating.toString(),
                accent = MeruTeal,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .scale(streakScale.value)
                .clip(RoundedCornerShape(16.dp))
                .background(MeruElevated)
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Streak flame", color = MeruMuted, fontSize = 12.sp)
                Text(
                    "${progression.streakDays} day${if (progression.streakDays == 1) "" else "s"}",
                    color = MeruAmber,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                if (progression.competitiveEligible) "Board eligible" else "Integrity gate",
                color = if (progression.competitiveEligible) MeruTeal else MeruAmber,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(12.dp))
        if (ranks.isNotEmpty()) {
            Text("SEASON RANKS", color = MeruMuted, fontSize = 11.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ranks.take(4).forEach { r ->
                    val delta = when {
                        r.delta > 0 -> "▲${r.delta}"
                        r.delta < 0 -> "▼${-r.delta}"
                        else -> ""
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MeruElevated)
                            .clickable(enabled = !driving, onClick = onOpenLeaderboards)
                            .padding(10.dp),
                    ) {
                        Text(r.geoType.take(4).uppercase(Locale.US), color = MeruMuted, fontSize = 10.sp)
                        Text(
                            r.rank?.let { "#$it" } ?: "—",
                            color = MeruTeal,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        if (delta.isNotEmpty()) {
                            Text(delta, color = MeruAmber, fontSize = 10.sp)
                        }
                    }
                }
            }
            if (driving) {
                Spacer(Modifier.height(4.dp))
                Text("Boards locked while driving", color = MeruAmber, fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        challenge?.let { c ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MeruElevated)
                    .clickable(onClick = onOpenChallenges)
                    .padding(14.dp),
            ) {
                Text("NEXT CHALLENGE", color = MeruTeal, fontSize = 11.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(4.dp))
                Text(c.title, color = MeruText, fontWeight = FontWeight.SemiBold)
                Text(c.description, color = MeruMuted, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                val p = if (c.target <= 0) 0f else (c.progress / c.target).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { p },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    color = MeruAmber,
                    trackColor = MeruVoid,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    String.format(Locale.US, "%.0f / %.0f · +%d XP", c.progress, c.target, c.xpReward),
                    color = MeruMuted,
                    fontSize = 12.sp,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatChip(Modifier.weight(1f), "Trips", local.tripCount.toString())
            StatChip(Modifier.weight(1f), "Cells", local.cells.toString())
        }
        Spacer(Modifier.height(12.dp))
        StatChip(
            Modifier.fillMaxWidth(),
            "Distance",
            String.format(Locale.US, "%.1f km", local.distanceKm),
        )

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            HubLink(Modifier.weight(1f), "Achievements", onOpenAchievements)
            HubLink(Modifier.weight(1f), "Challenges", onOpenChallenges)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            HubLink(
                Modifier.weight(1f),
                if (driving) "Arena locked" else "Arena",
                onClick = { if (!driving) onOpenLeaderboards() },
            )
            HubLink(
                Modifier.weight(1f),
                if (driving) "Map locked" else "Adventure map",
                onClick = { if (!driving) onOpenAdventureMap() },
            )
        }

        Spacer(Modifier.height(24.dp))
        Text("Next ascent", color = MeruText, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Drive, seal, then watch server XP and unlocks land on Home.",
            color = MeruMuted,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ScoreChip(
    modifier: Modifier,
    label: String,
    value: String,
    accent: androidx.compose.ui.graphics.Color,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MeruElevated)
            .padding(14.dp),
    ) {
        Text(label, color = MeruMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatChip(modifier: Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MeruElevated)
            .padding(14.dp),
    ) {
        Text(label, color = MeruMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = MeruText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HubLink(modifier: Modifier, label: String, onClick: () -> Unit) {
    Text(
        text = label,
        color = MeruVoid,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MeruTeal)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 12.dp),
    )
}
