package app.meru.android.feature.progression

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import app.meru.android.core.designsystem.theme.rememberReduceMotion
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruAmber
import app.meru.android.core.designsystem.theme.MeruCyan
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.core.network.AchievementItemDto
import app.meru.android.core.network.ChallengeItemDto
import app.meru.android.core.network.MeruApi
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class ProgressionListsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: MeruApi,
) : ViewModel() {
    private val _achievements = MutableStateFlow<List<AchievementItemDto>>(emptyList())
    val achievements: StateFlow<List<AchievementItemDto>> = _achievements.asStateFlow()

    private val _challenges = MutableStateFlow<List<ChallengeItemDto>>(emptyList())
    val challenges: StateFlow<List<ChallengeItemDto>> = _challenges.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                _achievements.value = api.achievementsMe("Bearer $token").items
                _challenges.value = api.challenges("Bearer $token").items
                _error.value = null
            }.onFailure {
                _error.value = "Could not refresh — try again online"
            }
        }
    }
}

@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    viewModel: ProgressionListsViewModel = hiltViewModel(),
) {
    val items by viewModel.achievements.collectAsState()
    val error by viewModel.error.collectAsState()
    val reduceMotion = rememberReduceMotion()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text("Achievements", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier = Modifier.height(8.dp))
        Text("Rarity frames unlock after server finalize", color = MeruMuted)
        error?.let {
            Spacer(Modifier = Modifier.height(8.dp))
            Text(it, color = MeruAmber, fontSize = 13.sp)
        }
        Spacer(Modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(items, key = { it.id }) { item ->
                val scale by animateFloatAsState(
                    targetValue = if (item.unlocked && !reduceMotion) 1f else 0.98f,
                    animationSpec = tween(400),
                    label = "achScale",
                )
                val frame = rarityColor(item.rarity)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(14.dp))
                        .background(MeruElevated)
                        .border(2.dp, if (item.unlocked) frame else MeruMuted.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(14.dp),
                ) {
                    Row(Modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(item.title, color = MeruText, fontWeight = FontWeight.SemiBold)
                        Text(
                            item.rarity.uppercase(Locale.US),
                            color = frame,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier = Modifier.height(4.dp))
                    Text(item.description, color = MeruMuted, fontSize = 13.sp)
                    Spacer(Modifier = Modifier.height(6.dp))
                    Text(
                        if (item.unlocked) "Unlocked · +${item.xpBonus} XP" else "Locked · +${item.xpBonus} XP",
                        color = if (item.unlocked) MeruTeal else MeruMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Spacer(Modifier = Modifier.height(12.dp))
        MeruSecondaryButton(text = "Back", onClick = onBack)
    }
}

@Composable
fun ChallengesScreen(
    onBack: () -> Unit,
    viewModel: ProgressionListsViewModel = hiltViewModel(),
) {
    val items by viewModel.challenges.collectAsState()
    val error by viewModel.error.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text("Challenges", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier = Modifier.height(8.dp))
        Text("Daily & weekly — progress ticks on trip finalize", color = MeruMuted)
        error?.let {
            Spacer(Modifier = Modifier.height(8.dp))
            Text(it, color = MeruAmber, fontSize = 13.sp)
        }
        Spacer(Modifier = Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            items(items, key = { it.id }) { c ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MeruElevated)
                        .padding(14.dp),
                ) {
                    Row(Modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(c.title, color = MeruText, fontWeight = FontWeight.SemiBold)
                        Text(c.period.uppercase(Locale.US), color = MeruCyan, fontSize = 11.sp)
                    }
                    Spacer(Modifier = Modifier.height(4.dp))
                    Text(c.description, color = MeruMuted, fontSize = 13.sp)
                    Spacer(Modifier = Modifier.height(8.dp))
                    val p = if (c.target <= 0) 0f else (c.progress / c.target).toFloat().coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        color = if (c.completed) MeruTeal else MeruAmber,
                        trackColor = MeruVoid,
                    )
                    Spacer(Modifier = Modifier.height(6.dp))
                    Text(
                        String.format(
                            Locale.US,
                            "%.0f / %.0f · +%d XP%s",
                            c.progress,
                            c.target,
                            c.xpReward,
                            if (c.completed) " · done" else "",
                        ),
                        color = MeruMuted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        Spacer(Modifier = Modifier.height(12.dp))
        MeruSecondaryButton(text = "Back", onClick = onBack)
    }
}

private fun rarityColor(rarity: String) = when (rarity.lowercase(Locale.US)) {
    "legendary" -> MeruAmber
    "epic" -> MeruCyan
    "rare" -> MeruTeal
    else -> MeruMuted
}
