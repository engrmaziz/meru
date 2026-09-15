package app.meru.android.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.database.ExplorationDao
import app.meru.android.core.database.TripDao
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeStats(
    val tripCount: Int = 0,
    val distanceKm: Double = 0.0,
    val bestQuality: Int = 0,
    val longestKm: Double = 0.0,
    val cells: Int = 0,
    val xp: Int = 0,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionStore: SessionStore,
    private val tripDao: TripDao,
    private val explorationDao: ExplorationDao,
) : ViewModel() {
    val session = sessionStore.session.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )

    private val _stats = MutableStateFlow(HomeStats())
    val stats: StateFlow<HomeStats> = _stats.asStateFlow()

    init {
        viewModelScope.launch {
            tripDao.observeCompletedTrips().collect { trips ->
                val xp = trips.sumOf { it.explorationXp }
                _stats.value = HomeStats(
                    tripCount = trips.size,
                    distanceKm = trips.sumOf { it.distanceM } / 1000.0,
                    bestQuality = trips.maxOfOrNull { it.qualityScore } ?: 0,
                    longestKm = (trips.maxOfOrNull { it.distanceM } ?: 0.0) / 1000.0,
                    cells = explorationDao.cellCount(),
                    xp = xp,
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val name = session?.displayName ?: "Driver"
    val level = 1 + stats.xp / 1000
    val intoLevel = stats.xp % 1000
    val progress = intoLevel / 1000f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text("Good to see you,", color = MeruMuted, fontSize = 14.sp)
        Text(name, color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MeruElevated)
                .padding(18.dp),
        ) {
            Text("LEVEL $level", color = MeruTeal, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MeruTeal,
                trackColor = MeruVoid,
            )
            Spacer(Modifier.height(8.dp))
            Text("$intoLevel / 1000 XP toward next", color = MeruMuted, fontSize = 13.sp)
        }

        Spacer(modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatChip(
                modifier = Modifier.weight(1f),
                label = "Distance",
                value = String.format(Locale.US, "%.1f km", stats.distanceKm),
            )
            StatChip(
                modifier = Modifier.weight(1f),
                label = "Best Q",
                value = if (stats.bestQuality > 0) stats.bestQuality.toString() else "—",
            )
        }
        Spacer(modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatChip(modifier = Modifier.weight(1f), label = "Trips", value = stats.tripCount.toString())
            StatChip(modifier = Modifier.weight(1f), label = "Cells", value = stats.cells.toString())
        }
        Spacer(modifier.height(12.dp))
        StatChip(
            modifier = Modifier.fillMaxWidth(),
            label = "Longest ascent",
            value = String.format(Locale.US, "%.2f km", stats.longestKm),
        )

        Spacer(modifier.height(24.dp))
        Text("Next ascent", color = MeruText, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Open Drive — after each trip, Afterglow seals score, XP, and sync.",
            color = MeruMuted,
        )
    }
}

@Composable
private fun StatChip(
    modifier: Modifier,
    label: String,
    value: String,
) {
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
