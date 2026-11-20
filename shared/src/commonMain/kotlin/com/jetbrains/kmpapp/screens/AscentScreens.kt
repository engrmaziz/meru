package com.jetbrains.kmpapp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jetbrains.kmpapp.data.AscentBackend
import com.jetbrains.kmpapp.data.AscentHome
import com.jetbrains.kmpapp.data.Chronicle
import com.jetbrains.kmpapp.data.ContractBoard
import com.jetbrains.kmpapp.data.ConvoyView
import com.jetbrains.kmpapp.data.DriveReceipt
import com.jetbrains.kmpapp.data.ExpeditionView
import com.jetbrains.kmpapp.data.RelicAlbum
import com.jetbrains.kmpapp.data.RitualView
import com.jetbrains.kmpapp.data.RoadCard
import com.jetbrains.kmpapp.data.RoadDetail
import com.jetbrains.kmpapp.data.Story
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.compose.viewmodel.koinViewModel

class AscentViewModel(private val backend: AscentBackend) : ViewModel() {
    private val _home = MutableStateFlow<AscentHome?>(null)
    val home: StateFlow<AscentHome?> = _home.asStateFlow()
    private val _roads = MutableStateFlow<List<RoadCard>>(emptyList())
    val roads: StateFlow<List<RoadCard>> = _roads.asStateFlow()
    private val _road = MutableStateFlow<RoadDetail?>(null)
    val road: StateFlow<RoadDetail?> = _road.asStateFlow()
    private val _expeditions = MutableStateFlow<List<ExpeditionView>>(emptyList())
    val expeditions: StateFlow<List<ExpeditionView>> = _expeditions.asStateFlow()
    private val _expedition = MutableStateFlow<ExpeditionView?>(null)
    val expedition: StateFlow<ExpeditionView?> = _expedition.asStateFlow()
    private val _relics = MutableStateFlow<RelicAlbum?>(null)
    val relics: StateFlow<RelicAlbum?> = _relics.asStateFlow()
    private val _rituals = MutableStateFlow<List<RitualView>>(emptyList())
    val rituals: StateFlow<List<RitualView>> = _rituals.asStateFlow()
    private val _contracts = MutableStateFlow<ContractBoard?>(null)
    val contracts: StateFlow<ContractBoard?> = _contracts.asStateFlow()
    private val _chronicle = MutableStateFlow<Chronicle?>(null)
    val chronicle: StateFlow<Chronicle?> = _chronicle.asStateFlow()
    private val _story = MutableStateFlow<Story?>(null)
    val story: StateFlow<Story?> = _story.asStateFlow()
    private val _convoys = MutableStateFlow<List<ConvoyView>>(emptyList())
    val convoys: StateFlow<List<ConvoyView>> = _convoys.asStateFlow()
    private val _receipt = MutableStateFlow<DriveReceipt?>(null)
    val receipt: StateFlow<DriveReceipt?> = _receipt.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        refreshHome()
    }

    fun refreshHome() = run { _home.value = backend.home() }
    fun loadRoads() = run { _roads.value = backend.roads().items }
    fun loadRoad(id: String) = run { _road.value = backend.road(id); _receipt.value = null }
    fun loadExpeditions() = run { _expeditions.value = backend.expeditions() }
    fun loadExpedition(id: String) = run { _expedition.value = backend.expedition(id) }
    fun embark(id: String) = run { _expedition.value = backend.embark(id) }
    fun loadRelics() = run { _relics.value = backend.relics() }
    fun loadRituals() = run {
        _rituals.value = backend.rituals()
        _contracts.value = backend.contracts()
    }
    fun pledge(id: String) = run { backend.pledge(id); loadRituals() }
    fun loadChronicle() = run { _chronicle.value = backend.chronicle() }
    fun loadStory() = run { _story.value = backend.story() }
    fun loadConvoys() = run { _convoys.value = backend.myConvoys() }
    fun createConvoy(name: String) = guard { backend.createConvoy(name); loadConvoys() }
    fun joinConvoy(code: String) = guard { backend.joinConvoy(code); loadConvoys() }
    fun drive(roadId: String) = guard {
        _receipt.value = backend.driveRoad(roadId)
        _road.value = backend.road(roadId)
        _home.value = backend.home()
    }
    fun stampCare() = guard {
        backend.noteCare("Canal Workshop")
        _chronicle.value = backend.chronicle()
    }

    private fun run(block: () -> Unit) {
        _error.value = runCatching(block).exceptionOrNull()?.message
    }

    private fun guard(block: () -> Unit) {
        val failure = runCatching(block).exceptionOrNull()
        _error.value = failure?.message
    }
}

@Composable
fun AscentHallScreen(
    onAtlas: () -> Unit,
    onExpeditions: () -> Unit,
    onRelics: () -> Unit,
    onChronicle: () -> Unit,
    onConvoy: () -> Unit,
    onContracts: () -> Unit,
    onStory: () -> Unit,
) {
    val viewModel = koinViewModel<AscentViewModel>()
    val homeState by viewModel.home.collectAsStateWithLifecycle()
    val home = homeState
    if (home == null) {
        Text("Sign in from the welcome screen. The atlas opens after the car has a driver.")
        return
    }
    Page {
        Text("Meru", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text(home.greeting, style = MaterialTheme.typography.titleMedium)
        Text(home.pitch, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Card {
            Text(home.window.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            Text(home.window.line)
            Text("Bond ${home.bond.level} · ${home.bond.title}")
            Text("${home.roadsClaimed}/${home.roadsTotal} roads · ${home.relicsFound}/${home.relicsTotal} relics · ${home.verifiedKm} verified km")
        }
        home.nextExpedition?.let {
            Card {
                Text(it.name, fontWeight = FontWeight.SemiBold)
                Text(it.pitch, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${it.closedChapters}/${it.chapters} chapters")
            }
        }
        home.nearestRelic?.let {
            Text("Still out there: ${it.name} on ${it.roadName}", color = MaterialTheme.colorScheme.tertiary)
        }
        Action("Missions", onAtlas)
        Action("Expeditions", onExpeditions)
        Action("Relic album", onRelics)
        Action("Car chronicle", onChronicle)
        Action("Convoy", onConvoy)
        Action("Rituals and contracts", onContracts)
        Action("Latest story", onStory)
    }
}

@Composable
fun AtlasScreen(onBack: () -> Unit, onRoad: (String) -> Unit) {
    val viewModel = koinViewModel<AscentViewModel>()
    val roads by viewModel.roads.collectAsStateWithLifecycle()
    remember(Unit) { viewModel.loadRoads() }
    Page {
        Action("Back", onBack)
        Text("Missions", style = MaterialTheme.typography.headlineMedium)
        Text("Sixteen roads. A mission closes only when a real drive passes it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        roads.forEach { road ->
            Card(onClick = { onRoad(road.id) }) {
                Text(road.name, fontWeight = FontWeight.SemiBold)
                Text("${road.region} · ${road.difficulty} · ${road.distanceKm} km", color = MaterialTheme.colorScheme.primary)
                Text(road.hook, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RoadScreen(roadId: String, onBack: () -> Unit) {
    val viewModel = koinViewModel<AscentViewModel>()
    val road by viewModel.road.collectAsStateWithLifecycle()
    val receipt by viewModel.receipt.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    remember(roadId) { viewModel.loadRoad(roadId) }
    Page {
        Action("Back", onBack)
        val detail = road
        if (detail == null || detail.card.id != roadId) {
            Text("Opening the road…")
        } else {
            Text(detail.card.name, style = MaterialTheme.typography.headlineMedium)
            Text(detail.card.region, color = MaterialTheme.colorScheme.primary)
            Text(detail.card.legend)
            Text(detail.card.hook, color = MaterialTheme.colorScheme.tertiary)
            Text(if (detail.claimed) "Claimed · driven ${detail.times} time(s)" else "Not claimed yet. Drive it for real.")
            detail.keeper?.let {
                Text(if (it.yours) "You keep this road at ${it.quality}." else "${it.displayName} keeps it at ${it.quality}.")
            }
            Text(
                "Start a drive and pass this road. One invented point cannot claim it or a relic on it.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            receipt?.let {
                Card {
                    Text(it.story.title, fontWeight = FontWeight.SemiBold)
                    Text(it.story.body)
                    Text("${it.verifiedKm} km · climb ${it.elevationGainM} m · bond ${it.bond.level}")
                }
            }
            detail.relics.forEach { relic ->
                Card {
                    Text(relic.name, fontWeight = FontWeight.SemiBold, color = if (relic.found) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    Text(relic.line, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (relic.found) "In the album" else "Still out there")
                }
            }
        }
    }
}

@Composable
fun ExpeditionsScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val viewModel = koinViewModel<AscentViewModel>()
    val items by viewModel.expeditions.collectAsStateWithLifecycle()
    remember(Unit) { viewModel.loadExpeditions() }
    Page {
        Action("Back", onBack)
        Text("Expeditions", style = MaterialTheme.typography.headlineMedium)
        items.forEach { exp ->
            Card(onClick = { onOpen(exp.id) }) {
                Text(exp.name, fontWeight = FontWeight.SemiBold)
                Text(exp.pitch, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${exp.closedChapters}/${exp.chapters.size} chapters" + if (exp.complete) " · complete" else "")
            }
        }
    }
}

@Composable
fun ExpeditionScreen(expeditionId: String, onBack: () -> Unit) {
    val viewModel = koinViewModel<AscentViewModel>()
    val exp by viewModel.expedition.collectAsStateWithLifecycle()
    remember(expeditionId) { viewModel.loadExpedition(expeditionId) }
    Page {
        Action("Back", onBack)
        val detail = exp
        if (detail == null || detail.id != expeditionId) {
            Text("Opening the expedition…")
        } else {
            Text(detail.name, style = MaterialTheme.typography.headlineMedium)
            Text(detail.pitch, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Reward title: ${detail.titleReward}", color = MaterialTheme.colorScheme.tertiary)
            if (!detail.embarked) Action("Take the oath") { viewModel.embark(detail.id) }
            else Text("You are on this expedition.", color = MaterialTheme.colorScheme.primary)
            detail.chapters.forEach { chapter ->
                Card {
                    Text(chapter.title, fontWeight = FontWeight.SemiBold)
                    Text(chapter.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (chapter.closed) "Closed" else "Open")
                }
            }
        }
    }
}

@Composable
fun RelicsScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<AscentViewModel>()
    val album by viewModel.relics.collectAsStateWithLifecycle()
    remember(Unit) { viewModel.loadRelics() }
    Page {
        Action("Back", onBack)
        Text("Relic album", style = MaterialTheme.typography.headlineMedium)
        Text("${album?.found ?: 0} of ${album?.total ?: 0} stood beside.")
        album?.items.orEmpty().forEach { relic ->
            Card {
                Text(relic.name, fontWeight = FontWeight.SemiBold, color = if (relic.found) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                Text(relic.roadName, color = MaterialTheme.colorScheme.tertiary)
                Text(if (relic.found) relic.line else "Drive the ${relic.roadName} to uncover this.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ChronicleScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<AscentViewModel>()
    val book by viewModel.chronicle.collectAsStateWithLifecycle()
    remember(Unit) { viewModel.loadChronicle() }
    Page {
        Action("Back", onBack)
        Text("Chronicle", style = MaterialTheme.typography.headlineMedium)
        val chronicle = book
        if (chronicle == null) {
            Text("Reading the logbook…")
        } else {
            Text("Bond ${chronicle.bond.level} · ${chronicle.bond.title}", color = MaterialTheme.colorScheme.primary)
            Text("${chronicle.verifiedKm} verified km · ${chronicle.roadsClaimed} roads · ${chronicle.relicsFound} relics")
            if (chronicle.titles.isEmpty()) Text("Titles arrive when the car has been somewhere.")
            chronicle.titles.forEach { Text(it, color = MaterialTheme.colorScheme.tertiary) }
            chronicle.careMarks.forEach { Text("Cared for at ${it.workshop}") }
            Action("Record a workshop visit") { viewModel.stampCare() }
            chronicle.latestStory?.let {
                Card {
                    Text(it.title, fontWeight = FontWeight.SemiBold)
                    Text(it.body)
                }
            }
        }
    }
}

@Composable
fun ConvoyScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<AscentViewModel>()
    val convoys by viewModel.convoys.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("Sunday Company") }
    var code by remember { mutableStateOf("") }
    remember(Unit) { viewModel.loadConvoys() }
    Page {
        Action("Back", onBack)
        Text("Convoy", style = MaterialTheme.typography.headlineMedium)
        Text("Kilometres shared with people who actually showed up.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        convoys.forEach { convoy ->
            Card {
                Text(convoy.name, fontWeight = FontWeight.SemiBold)
                Text("Code ${convoy.code} · ${convoy.members} drivers · ${convoy.sharedKm} km" + if (convoy.leader) " · you lead" else "", color = MaterialTheme.colorScheme.primary)
                Text(convoy.oath, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(convoy.roster.joinToString())
            }
        }
        OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Convoy name") })
        Action("Start a convoy") { viewModel.createConvoy(name) }
        OutlinedTextField(code, { code = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Join code") })
        Action("Join with a code") { if (code.isNotBlank()) viewModel.joinConvoy(code) }
    }
}

@Composable
fun ContractsScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<AscentViewModel>()
    val rituals by viewModel.rituals.collectAsStateWithLifecycle()
    val contracts by viewModel.contracts.collectAsStateWithLifecycle()
    remember(Unit) { viewModel.loadRituals() }
    Page {
        Action("Back", onBack)
        Text("Rituals", style = MaterialTheme.typography.headlineMedium)
        rituals.forEach { ritual ->
            Card {
                Text(ritual.name, fontWeight = FontWeight.SemiBold)
                Text(ritual.vow, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (ritual.pledged) "Pledged · streak ${ritual.streak}" else "Not pledged")
                if (!ritual.pledged) Action("Pledge") { viewModel.pledge(ritual.id) }
            }
        }
        Text("This week's contracts", style = MaterialTheme.typography.titleLarge)
        Text(contracts?.week.orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant)
        contracts?.items.orEmpty().forEach { contract ->
            Card {
                Text(contract.name, fontWeight = FontWeight.SemiBold)
                Text(contract.demand, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${contract.progress} / ${contract.target}")
            }
        }
    }
}

@Composable
fun StoryScreen(onBack: () -> Unit) {
    val viewModel = koinViewModel<AscentViewModel>()
    val story by viewModel.story.collectAsStateWithLifecycle()
    remember(Unit) { viewModel.loadStory() }
    Page {
        Action("Back", onBack)
        Text(story?.title ?: "The latest page", style = MaterialTheme.typography.headlineMedium)
        Text(story?.body ?: "Drive, then come back. The page writes itself from the trace.")
    }
}

@Composable
private fun Page(content: @Composable () -> Unit) {
    MeruColumn { content() }
}

@Composable
private fun Card(onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xC0141C18))
            .border(1.dp, Color(0x33E8A317), RoundedCornerShape(20.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = ripple(color = MeruAmber.copy(alpha = 0.25f)), onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun Action(label: String, onClick: () -> Unit) {
    MeruButton(label, onClick = onClick)
}
