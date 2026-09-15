package app.meru.android.feature.arena

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.meru.android.core.designsystem.theme.rememberReduceMotion
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.database.ExplorationDao
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.components.MeruPrimaryButton
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruAmber
import app.meru.android.core.designsystem.theme.MeruCyan
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.core.network.BoardEntryDto
import app.meru.android.core.network.ExplorationMapResponse
import app.meru.android.core.network.LeaderboardResponse
import app.meru.android.core.network.MeruApi
import app.meru.android.core.network.PrivacyPatchRequest
import app.meru.android.core.network.ShareCardResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private data class GeoTab(val label: String, val geoType: String, val geoId: String)

private val geoTabs = listOf(
    GeoTab("City", "city", "pk-pb-lhr"),
    GeoTab("Province", "province", "pk-pb"),
    GeoTab("Country", "country", "pk"),
    GeoTab("Global", "global", "global"),
)

@HiltViewModel
class ArenaViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: MeruApi,
    private val explorationDao: ExplorationDao,
) : ViewModel() {
    private val _board = MutableStateFlow<LeaderboardResponse?>(null)
    val board: StateFlow<LeaderboardResponse?> = _board.asStateFlow()

    private val _friends = MutableStateFlow<List<BoardEntryDto>>(emptyList())
    val friends: StateFlow<List<BoardEntryDto>> = _friends.asStateFlow()

    private val _exploration = MutableStateFlow<ExplorationMapResponse?>(null)
    val exploration: StateFlow<ExplorationMapResponse?> = _exploration.asStateFlow()

    private val _localCells = MutableStateFlow(0)
    val localCells: StateFlow<Int> = _localCells.asStateFlow()

    private val _share = MutableStateFlow<ShareCardResponse?>(null)
    val share: StateFlow<ShareCardResponse?> = _share.asStateFlow()

    private val _boardOptIn = MutableStateFlow(true)
    val boardOptIn: StateFlow<Boolean> = _boardOptIn.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _tab = MutableStateFlow(0)
    val tab: StateFlow<Int> = _tab.asStateFlow()

    init {
        viewModelScope.launch {
            _localCells.value = explorationDao.cellCount()
        }
    }

    fun selectTab(index: Int) {
        _tab.value = index
        loadBoard()
    }

    fun loadBoard() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            val t = geoTabs[_tab.value.coerceIn(0, geoTabs.lastIndex)]
            runCatching {
                _board.value = api.leaderboard("Bearer $token", t.geoType, t.geoId)
                _friends.value = api.friendsLeaderboard("Bearer $token").entries
                _boardOptIn.value = api.privacyMe("Bearer $token").boardOptIn
                _error.value = null
            }.onFailure {
                _error.value = "Boards offline — retry when connected"
            }
        }
    }

    fun loadExploration() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                _exploration.value = api.explorationMap("Bearer $token")
                _localCells.value = explorationDao.cellCount()
            }.onFailure {
                _error.value = "Map offline"
            }
        }
    }

    fun loadShare() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                _share.value = api.shareCard("Bearer $token")
            }
        }
    }

    fun setBoardOptIn(optIn: Boolean) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                val res = api.privacyPatch("Bearer $token", PrivacyPatchRequest(boardOptIn = optIn))
                _boardOptIn.value = res.boardOptIn
                loadBoard()
            }
        }
    }

    fun follow(targetId: String) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                api.follow("Bearer $token", targetId)
                _friends.value = api.friendsLeaderboard("Bearer $token").entries
            }
        }
    }
}

@Composable
fun LeaderboardsScreen(
    onBack: () -> Unit,
    onOpenAdventureMap: () -> Unit,
    onOpenShare: () -> Unit,
    viewModel: ArenaViewModel = hiltViewModel(),
) {
    val board by viewModel.board.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val tab by viewModel.tab.collectAsState()
    val error by viewModel.error.collectAsState()
    val optIn by viewModel.boardOptIn.collectAsState()
    var showFriends by remember { mutableStateOf(false) }
    val reduceMotion = rememberReduceMotion()

    LaunchedEffect(Unit) { viewModel.loadBoard() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text("Arena", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text(
            board?.seasonName ?: "Ascent Season",
            color = MeruTeal,
            fontSize = 14.sp,
        )
        error?.let {
            Spacer(Modifier = Modifier.height(6.dp))
            Text(it, color = MeruAmber, fontSize = 12.sp)
        }
        Spacer(Modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            geoTabs.forEachIndexed { i, g ->
                FilterChip(
                    selected = tab == i && !showFriends,
                    onClick = {
                        showFriends = false
                        viewModel.selectTab(i)
                    },
                    label = { Text(g.label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeruTeal,
                        selectedLabelColor = MeruVoid,
                        containerColor = MeruElevated,
                        labelColor = MeruText,
                    ),
                )
            }
        }
        Spacer(Modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = showFriends,
                onClick = { showFriends = true },
                label = { Text("Friends", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MeruCyan,
                    selectedLabelColor = MeruVoid,
                    containerColor = MeruElevated,
                    labelColor = MeruText,
                ),
            )
            FilterChip(
                selected = false,
                onClick = onOpenAdventureMap,
                label = { Text("Map", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MeruElevated,
                    labelColor = MeruText,
                ),
            )
            FilterChip(
                selected = false,
                onClick = onOpenShare,
                label = { Text("Share", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MeruElevated,
                    labelColor = MeruText,
                ),
            )
        }

        if (!optIn) {
            Spacer(Modifier = Modifier.height(8.dp))
            Text("You are hidden from boards (privacy).", color = MeruAmber, fontSize = 12.sp)
        }

        Spacer(Modifier = Modifier.height(12.dp))

        if (!showFriends) {
            board?.podium?.takeIf { it.isNotEmpty() }?.let { podium ->
                PodiumRow(podium, reduceMotion)
                Spacer(Modifier = Modifier.height(12.dp))
            }
        }

        val list = if (showFriends) friends else board?.entries.orEmpty()
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(list, key = { it.userId + it.rank }) { entry ->
                BoardRow(
                    entry = entry,
                    highlight = entry.userId == board?.you?.userId,
                    onFollow = if (showFriends) null else ({ viewModel.follow(entry.userId) }),
                )
            }
        }

        board?.you?.takeIf { it.rank > 0 }?.let { you ->
            Spacer(Modifier = Modifier.height(8.dp))
            Text("YOU", color = MeruMuted, fontSize = 11.sp, letterSpacing = 1.sp)
            BoardRow(entry = you, highlight = true, sticky = true)
        }

        Spacer(Modifier = Modifier.height(12.dp))
        MeruSecondaryButton(text = "Back", onClick = onBack)
    }
}

@Composable
private fun PodiumRow(podium: List<BoardEntryDto>, reduceMotion: Boolean) {
    val scale = remember { Animatable(0.9f) }
    LaunchedEffect(podium) {
        if (!reduceMotion) {
            scale.snapTo(0.9f)
            scale.animateTo(1f, tween(500))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale.value; scaleY = scale.value },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom,
    ) {
        val order = listOfNotNull(podium.getOrNull(1), podium.getOrNull(0), podium.getOrNull(2))
        order.forEach { e ->
            val h = when (e.rank) {
                1 -> 88.dp
                2 -> 72.dp
                else -> 64.dp
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("#${e.rank}", color = MeruAmber, fontWeight = FontWeight.Bold)
                Box(
                    modifier = Modifier
                        .size(width = 72.dp, height = h)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MeruElevated)
                        .border(1.dp, MeruTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(e.displayName.take(8), color = MeruText, fontSize = 11.sp)
                        Text("${e.score}", color = MeruTeal, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoardRow(
    entry: BoardEntryDto,
    highlight: Boolean = false,
    sticky: Boolean = false,
    onFollow: (() -> Unit)? = null,
) {
    val deltaLabel = when {
        entry.delta > 0 -> "▲${entry.delta}"
        entry.delta < 0 -> "▼${-entry.delta}"
        else -> "—"
    }
    val deltaColor = when {
        entry.delta > 0 -> MeruTeal
        entry.delta < 0 -> MeruAmber
        else -> MeruMuted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (highlight || sticky) MeruElevated else MeruElevated.copy(alpha = 0.85f))
            .then(
                if (highlight) Modifier.border(1.dp, MeruTeal, RoundedCornerShape(12.dp))
                else Modifier,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (entry.rank > 0) "#${entry.rank}" else "—",
            color = MeruTeal,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 10.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.displayName, color = MeruText, fontWeight = FontWeight.Medium)
            Text(
                "Lv ${entry.level} · rating ${entry.driverRating}",
                color = MeruMuted,
                fontSize = 12.sp,
            )
        }
        Text(deltaLabel, color = deltaColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier = Modifier.size(10.dp))
        Text("${entry.score}", color = MeruText, fontWeight = FontWeight.Bold)
        onFollow?.let {
            Spacer(Modifier = Modifier.size(8.dp))
            Text(
                "Follow",
                color = MeruCyan,
                fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = it),
            )
        }
    }
}

@Composable
fun AdventureMapScreen(
    onBack: () -> Unit,
    viewModel: ArenaViewModel = hiltViewModel(),
) {
    val exploration by viewModel.exploration.collectAsState()
    val localCells by viewModel.localCells.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadExploration() }

    val pct = exploration?.cityPercent ?: 0.0
    val progress = (pct / 100.0).toFloat().coerceIn(0f, 1f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Adventure map", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text(
            exploration?.cityName ?: "Lahore",
            color = MeruTeal,
            fontSize = 14.sp,
        )
        Spacer(Modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MeruElevated)
                .padding(18.dp),
        ) {
            Text("CITY EXPLORED", color = MeruMuted, fontSize = 11.sp, letterSpacing = 1.sp)
            Text(
                String.format(Locale.US, "%.1f%%", pct),
                color = MeruText,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(10.dp)),
                color = MeruTeal,
                trackColor = MeruVoid,
            )
            Spacer(Modifier = Modifier.height(8.dp))
            Text(
                "Server cells ${exploration?.cellsExplored ?: 0} · local atlas $localCells",
                color = MeruMuted,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier = Modifier.height(16.dp))
        Text("Geohash glow", color = MeruText, fontWeight = FontWeight.Medium)
        Spacer(Modifier = Modifier.height(8.dp))
        val cells = exploration?.sampleCells.orEmpty()
        if (cells.isEmpty()) {
            Text("Drive to light up Lahore cells.", color = MeruMuted)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Grid of glow dots
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                cells.chunked(8).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { _ ->
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(MeruTeal.copy(alpha = 0.75f)),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier = Modifier.height(24.dp))
        MeruSecondaryButton(text = "Back", onClick = onBack)
    }
}

@Composable
fun ShareCardScreen(
    onBack: () -> Unit,
    viewModel: ArenaViewModel = hiltViewModel(),
) {
    val share by viewModel.share.collectAsState()
    val clipboard = LocalClipboardManager.current
    LaunchedEffect(Unit) { viewModel.loadShare() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text("Share card", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("Privacy-safe — no exact location", color = MeruMuted)
        Spacer(Modifier = Modifier.height(20.dp))

        share?.let { s ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MeruElevated)
                    .border(1.dp, MeruTeal.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(20.dp),
            ) {
                Text("MERU", color = MeruTeal, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(Modifier = Modifier.height(8.dp))
                Text(s.displayName, color = MeruText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                Text("${s.title} · Level ${s.level}", color = MeruCyan)
                Spacer(Modifier = Modifier.height(12.dp))
                Text("Adventure ${s.adventureScore}", color = MeruText, fontSize = 20.sp)
                Text(
                    "${s.cityName} rank ${s.cityRank ?: "—"} · ${s.seasonName}",
                    color = MeruMuted,
                    fontSize = 13.sp,
                )
                Spacer(Modifier = Modifier.height(12.dp))
                Text(s.tagline, color = MeruAmber, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier = Modifier.height(16.dp))
            MeruPrimaryButton(
                text = "Copy share text",
                onClick = {
                    val text = buildString {
                        appendLine("${s.displayName} · ${s.title} Lv${s.level}")
                        appendLine("Adventure ${s.adventureScore} · ${s.cityName} #${s.cityRank ?: "—"}")
                        appendLine(s.tagline)
                    }
                    clipboard.setText(AnnotatedString(text))
                },
            )
        }

        Spacer(Modifier = Modifier.height(12.dp))
        MeruSecondaryButton(text = "Back", onClick = onBack)
    }
}
