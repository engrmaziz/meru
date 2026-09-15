package app.meru.android.feature.trips

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.database.ExplorationDao
import app.meru.android.core.database.TripDao
import app.meru.android.core.database.TripEntity
import app.meru.android.core.database.TripEventEntity
import app.meru.android.core.database.TripLocationEntity
import app.meru.android.core.datastore.ProgressionStore
import app.meru.android.core.designsystem.components.MeruPrimaryButton
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruAmber
import app.meru.android.core.designsystem.theme.MeruCyan
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.core.network.TripAwardsDto
import app.meru.android.engine.telemetry.RoutePoint
import app.meru.android.feature.drive.DriveMap
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.max

data class TripDetailState(
    val trip: TripEntity? = null,
    val locations: List<TripLocationEntity> = emptyList(),
    val events: List<TripEventEntity> = emptyList(),
    val loading: Boolean = true,
    val totalCells: Int = 0,
)

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tripDao: TripDao,
    private val explorationDao: ExplorationDao,
    progressionStore: ProgressionStore,
) : ViewModel() {
    private val tripId: String = checkNotNull(savedStateHandle["tripId"])

    private val _state = MutableStateFlow(TripDetailState())
    val state: StateFlow<TripDetailState> = _state.asStateFlow()

    val serverAwards: StateFlow<TripAwardsDto?> = progressionStore.snapshot
        .map { snap ->
            snap.lastAwards?.takeIf { it.clientTripId == tripId }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val ghostMessage: StateFlow<String?> = progressionStore.snapshot
        .map { it.lastGhostMessage }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            val trip = tripDao.getTrip(tripId)
            val locs = tripDao.locationsForTrip(tripId)
            val events = tripDao.eventsForTrip(tripId)
            val cells = explorationDao.cellCount()
            _state.value = TripDetailState(
                trip = trip,
                locations = locs,
                events = events,
                loading = false,
                totalCells = cells,
            )
            // Refresh trip row when sync overlays server quality
            while (true) {
                delay(1500)
                val refreshed = tripDao.getTrip(tripId) ?: break
                if (refreshed != _state.value.trip) {
                    _state.value = _state.value.copy(trip = refreshed)
                }
                if (refreshed.syncStatus == "synced") break
            }
        }
    }
}

@Composable
fun TripProcessingScreen(
    tripId: String,
    onReady: (String) -> Unit,
) {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(tripId) {
        val steps = listOf(0.2f, 0.45f, 0.7f, 0.92f, 1f)
        for (p in steps) {
            progress = p
            delay(280)
        }
        onReady(tripId)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MeruTeal)
            Spacer(modifier = Modifier.height(20.dp))
            Text("Sealing your ascent", color = MeruText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Route · events · exploration", color = MeruMuted)
            Spacer(modifier = Modifier.height(24.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MeruTeal,
                trackColor = MeruElevated,
            )
        }
    }
}

@Composable
fun TripSummaryScreen(
    onOpenDetail: (String) -> Unit,
    onDone: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val awards by viewModel.serverAwards.collectAsState()
    val ghostMsg by viewModel.ghostMessage.collectAsState()
    val trip = state.trip

    if (state.loading || trip == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MeruVoid),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MeruTeal)
        }
        return
    }

    val displayQuality = awards?.qualityScore ?: trip.qualityScore
    val displayXp = awards?.xpAwarded ?: trip.explorationXp
    val isFinal = awards != null || trip.syncStatus == "synced"
    val scoreAnim = remember { Animatable(0f) }
    LaunchedEffect(displayQuality) {
        scoreAnim.snapTo(0f)
        scoreAnim.animateTo(displayQuality.toFloat(), tween(1200))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("ASCENT COMPLETE", color = MeruTeal, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text("Drive sealed", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text(
            if (isFinal) "Server final · spoofed client scores ignored"
            else "Provisional — syncing final XP…",
            color = if (isFinal) MeruCyan else MeruAmber,
            fontSize = 13.sp,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MeruElevated)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("QUALITY", color = MeruMuted, fontSize = 12.sp, letterSpacing = 2.sp)
            Text(
                scoreAnim.value.toInt().toString(),
                color = MeruText,
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                awards?.title ?: "Adventure seal",
                color = MeruAmber,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { (displayXp / 400f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = MeruTeal,
                trackColor = MeruVoid,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text("+$displayXp XP awarded", color = MeruMuted, fontSize = 13.sp)
            awards?.let { a ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Adventure ${a.adventureScore} · Rating ${a.driverRating} · Streak ${a.streakDays}",
                    color = MeruTeal,
                    fontSize = 12.sp,
                )
                if (!a.competitiveEligible) {
                    Text("Integrity gate — boards locked for this trip", color = MeruAmber, fontSize = 12.sp)
                }
                a.unlockedAchievements.forEach { unlock ->
                    Text(
                        "Unlocked: ${unlock.title} (+${unlock.xpBonus})",
                        color = MeruCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        ghostMsg?.let { msg ->
            Text(
                msg,
                color = MeruCyan,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MeruElevated)
                    .padding(12.dp),
            )
        }

        if (trip.newCells > 0) {
            Text(
                "New roads unlocked: ${trip.newCells} cells · atlas ${state.totalCells}",
                color = MeruTeal,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MeruElevated)
                    .padding(12.dp),
            )
        }

        Row(Modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStat("Distance", String.format(Locale.US, "%.2f km", trip.distanceM / 1000), Modifier.weight(1f))
            SummaryStat("Duration", formatDuration(trip.durationMs), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStat("Avg", String.format(Locale.US, "%.0f km/h", trip.avgSpeedKmh), Modifier.weight(1f))
            SummaryStat("Max", String.format(Locale.US, "%.0f km/h", trip.maxSpeedKmh), Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryStat("Climb", String.format(Locale.US, "%.0f m", trip.elevationGainM), Modifier.weight(1f))
            SummaryStat("Sync", trip.syncStatus, Modifier.weight(1f))
        }

        MeruPrimaryButton(text = "Open timeline & replay", onClick = { onOpenDetail(trip.id) })
        MeruSecondaryButton(text = "Back to Drive", onClick = onDone)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MeruElevated)
            .padding(14.dp),
    ) {
        Text(label, color = MeruMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, color = MeruText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

@Composable
fun TripDetailScreen(
    onBack: () -> Unit,
    viewModel: TripDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val trip = state.trip
    if (state.loading || trip == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MeruVoid),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MeruTeal)
        }
        return
    }

    val route = remember(state.locations) {
        state.locations.map { RoutePoint(it.latitude, it.longitude) }
    }
    var replayIndex by remember { mutableFloatStateOf(0f) }
    val idx = replayIndex.toInt().coerceIn(0, max(0, state.locations.lastIndex))
    val cursor = state.locations.getOrNull(idx)
    val visibleRoute = if (state.locations.isEmpty()) emptyList() else route.take(idx + 1)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Trip detail", color = MeruText, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(trip.startAtMs)),
            color = MeruMuted,
        )

        Text("Replay", color = MeruTeal, fontWeight = FontWeight.SemiBold)
        DriveMap(
            route = visibleRoute,
            latitude = cursor?.latitude,
            longitude = cursor?.longitude,
            bearing = cursor?.bearing,
            expanded = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        )
        if (state.locations.size > 1) {
            Slider(
                value = replayIndex,
                onValueChange = { replayIndex = it },
                valueRange = 0f..(state.locations.lastIndex.toFloat()),
                colors = SliderDefaults.colors(thumbColor = MeruTeal, activeTrackColor = MeruTeal),
            )
            Text(
                cursor?.let {
                    String.format(
                        Locale.US,
                        "t+%s · %.0f km/h",
                        formatDuration(it.timestampMs - trip.startAtMs),
                        (it.speedMps ?: 0.0) * 3.6,
                    )
                } ?: "—",
                color = MeruMuted,
                fontSize = 13.sp,
            )
        }

        Text("Speed", color = MeruTeal, fontWeight = FontWeight.SemiBold)
        MetricGraph(
            values = state.locations.map { (it.speedMps ?: 0.0) * 3.6 },
            color = MeruTeal,
        )

        Text("Elevation", color = MeruAmber, fontWeight = FontWeight.SemiBold)
        MetricGraph(
            values = state.locations.mapNotNull { it.altitudeM },
            color = MeruAmber,
        )

        Text("Timeline", color = MeruTeal, fontWeight = FontWeight.SemiBold)
        state.events.forEach { event ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MeruElevated)
                    .padding(12.dp),
            ) {
                Text(event.label, color = MeruText, fontWeight = FontWeight.Medium)
                Text(
                    "${event.type} · ${formatDuration(event.timestampMs - trip.startAtMs)}",
                    color = MeruMuted,
                    fontSize = 12.sp,
                )
            }
        }

        MeruSecondaryButton(text = "Back", onClick = onBack)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MetricGraph(
    values: List<Double>,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(120.dp),
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MeruElevated)
            .padding(12.dp),
    ) {
        if (values.size < 2) {
            Text("Not enough samples", color = MeruMuted, fontSize = 12.sp)
            return
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minV = values.minOrNull() ?: 0.0
            val maxV = values.maxOrNull() ?: 1.0
            val span = (maxV - minV).takeIf { it > 1e-6 } ?: 1.0
            val path = Path()
            values.forEachIndexed { i, v ->
                val x = size.width * i / (values.size - 1).toFloat()
                val y = size.height * (1f - ((v - minV) / span).toFloat())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = color, style = Stroke(width = 4f, cap = StrokeCap.Round))
            drawLine(MeruMuted.copy(alpha = 0.3f), Offset(0f, size.height), Offset(size.width, size.height), 1f)
        }
    }
}

@HiltViewModel
class TripsViewModel @Inject constructor(
    tripDao: TripDao,
) : ViewModel() {
    val trips = tripDao.observeCompletedTrips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun TripsListScreen(
    onOpenTrip: (String) -> Unit = {},
    viewModel: TripsViewModel = hiltViewModel(),
) {
    val trips by viewModel.trips.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text("Trips", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Local history · tap for replay", color = MeruMuted)
        Spacer(modifier = Modifier.height(16.dp))
        if (trips.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No trips yet. Start a drive to begin your ascent.", color = MeruMuted)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(trips, key = { it.id }) { trip ->
                    TripRow(trip, onClick = { onOpenTrip(trip.id) })
                }
            }
        }
    }
}

@Composable
private fun TripRow(trip: TripEntity, onClick: () -> Unit) {
    val fmt = SimpleDateFormat("MMM d · HH:mm", Locale.getDefault())
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MeruElevated)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(fmt.format(Date(trip.startAtMs)), color = MeruText, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            String.format(
                Locale.US,
                "%.2f km · Q%d · +%d XP · %s",
                trip.distanceM / 1000.0,
                trip.qualityScore,
                trip.explorationXp,
                trip.syncStatus,
            ),
            color = MeruMuted,
            fontSize = 13.sp,
        )
        if (trip.newCells > 0) {
            Text("New cells: ${trip.newCells}", color = MeruTeal, fontSize = 12.sp)
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0))
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
