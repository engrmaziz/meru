package com.jetbrains.kmpapp.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jetbrains.kmpapp.data.AscentBackend
import com.jetbrains.kmpapp.data.BoardRow
import com.jetbrains.kmpapp.data.DriveRecorder
import com.jetbrains.kmpapp.data.DriverProfile
import com.jetbrains.kmpapp.data.LiveDrive
import com.jetbrains.kmpapp.data.RideMark
import com.jetbrains.kmpapp.data.RideSample
import com.jetbrains.kmpapp.data.SavedDrive
import com.jetbrains.kmpapp.data.platformHasGps
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.compose.viewmodel.koinViewModel

class MeruViewModel(
    private val backend: AscentBackend,
    private val recorder: DriveRecorder,
) : ViewModel() {
    val live: StateFlow<LiveDrive> = recorder.live
    private val _revision = MutableStateFlow(0)
    val revision: StateFlow<Int> = _revision.asStateFlow()

    fun refresh() {
        _revision.value += 1
    }

    fun start() = recorder.start()

    fun stop(): SavedDrive? {
        val saved = recorder.stop()
        refresh()
        return saved
    }

    fun profile(): DriverProfile? = backend.profile()
    fun drives(): List<SavedDrive> = backend.drives()
    fun points(): Int = backend.points()
    fun board(tier: String): List<BoardRow> = backend.board(tier)
    fun checkpoint(): SavedDrive? = backend.openCheckpoint()
    fun drive(id: String): SavedDrive? = backend.drive(id)
    fun windowLine(): String = runCatching { backend.home().window.line }.getOrDefault("Start when you are ready.")

    fun keepCheckpoint(): SavedDrive? {
        val open = backend.openCheckpoint() ?: return null
        val saved = backend.acceptDrive(open)
        refresh()
        return saved
    }

    fun discardCheckpoint() {
        backend.clearCheckpoint()
        refresh()
    }

    fun create(
        name: String,
        email: String,
        pin: String,
        city: String,
        province: String,
        country: String,
        carName: String,
        make: String,
        model: String,
        year: Int,
        plate: String,
        referral: String,
    ) = backend.createDriver(name, email, pin, city, province, country, carName, make, model, year, plate, referral)

    fun login(email: String, pin: String) = backend.login(email, pin)

    fun signOut() {
        backend.signOut()
        refresh()
    }
}

@Composable
fun WelcomeScreen(onReady: () -> Unit) {
    val viewModel = koinViewModel<MeruViewModel>()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Lahore") }
    var province by remember { mutableStateOf("Punjab") }
    var country by remember { mutableStateOf("Pakistan") }
    var carName by remember { mutableStateOf("") }
    var make by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2020") }
    var plate by remember { mutableStateOf("") }
    var referral by remember { mutableStateOf("") }
    var signingIn by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    MeruColumn {
        Text("Meru", style = MaterialTheme.typography.displayLarge, color = MeruPaper)
        Text("The road keeps the score.", style = MaterialTheme.typography.headlineMedium, color = MeruAmber)
        Text(
            "One car on the free version. After this, every drive stays: the line, the brakes, the weather, the points.",
            color = MeruMist,
            style = MaterialTheme.typography.bodyLarge,
        )
        if (!signingIn) {
            MeruField("Your name", name) { name = it }
            MeruField("Email", email) { email = it }
            MeruField("Pin, 6 or more", pin) { pin = it }
            MeruField("City", city) { city = it }
            MeruField("Province or state", province) { province = it }
            MeruField("Country", country) { country = it }
            Text("The car", style = MaterialTheme.typography.titleMedium)
            MeruField("What you call it", carName) { carName = it }
            MeruField("Make", make) { make = it }
            MeruField("Model", model) { model = it }
            MeruField("Year", year) { year = it }
            MeruField("Plate", plate) { plate = it }
            MeruField("Referral code. MERU is the launch code", referral) { referral = it }
            MeruButton("This is my car") {
                val created = runCatching {
                    viewModel.create(
                        name, email, pin, city, province, country, carName, make, model,
                        year.toIntOrNull() ?: 2020, plate, referral,
                    )
                }
                created.onSuccess { onReady() }
                created.onFailure { message = it.message }
            }
            MeruButton("I already drive this car", primary = false) { signingIn = true }
        } else {
            MeruField("Email", email) { email = it }
            MeruField("Pin", pin) { pin = it }
            MeruButton("Sign in") {
                val signed = runCatching { viewModel.login(email, pin) }
                signed.onSuccess { onReady() }
                signed.onFailure { message = it.message }
            }
            MeruButton("New driver", primary = false) { signingIn = false }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun MeruShell(
    onSummary: (String) -> Unit,
    onMissions: () -> Unit,
    onExpeditions: () -> Unit,
    onRelics: () -> Unit,
    onChronicle: () -> Unit,
    onConvoy: () -> Unit,
    onContracts: () -> Unit,
    onStory: () -> Unit,
    onSignedOut: () -> Unit,
) {
    val viewModel = koinViewModel<MeruViewModel>()
    val live by viewModel.live.collectAsStateWithLifecycle()
    val revision by viewModel.revision.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("Drive") }
    var note by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        NightBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().background(Color.Transparent)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                "Drive" -> DriveTab(viewModel, live, revision, note, onNote = { note = it }) { saved ->
                    if (saved == null) note = "Stay with it a moment. A drive needs a trace."
                    else onSummary(saved.id)
                }
                "Log" -> LogTab(viewModel.drives(), onSummary)
                "Missions" -> MissionHome(onMissions, onExpeditions, onRelics)
                "Boards" -> BoardsTab(viewModel)
                else -> YouTab(viewModel, onChronicle, onConvoy, onContracts, onStory) {
                    viewModel.signOut()
                    onSignedOut()
                }
            }
        }
        MeruNav(tab) { tab = it }
        }
    }
}

@Composable
private fun DriveTab(
    viewModel: MeruViewModel,
    live: LiveDrive,
    revision: Int,
    note: String?,
    onNote: (String) -> Unit,
    onFinished: (SavedDrive?) -> Unit,
) {
    val checkpoint = remember(revision, live.active) { viewModel.checkpoint() }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (live.active) {
            SpeedFace(live.speedKmh.toInt(), elapsed(live.startedAt))
            if (live.note.isNotBlank()) Text(live.note, color = MeruMist, style = MaterialTheme.typography.bodyMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Instrument("Distance", km(live.distanceM), Modifier.weight(1f))
                Instrument("Peak", "${live.maxSpeedKmh.toInt()} km/h", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Instrument("G-force", g(live.longG, live.latG), Modifier.weight(1f))
                Instrument("Climb", "${live.elevationGainM.toInt()} m", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Instrument("Brakes", live.brakeCount.toString(), Modifier.weight(1f))
                Instrument("Lanes", live.laneChanges.toString(), Modifier.weight(1f))
            }
            val sky = live.sky
            Instrument(
                sky.label,
                "${sky.tempC?.let { "${it.toInt()}°" } ?: "—"}   AQI ${sky.aqi ?: "—"}   ${sky.windKmh?.toInt() ?: "—"} km/h",
            )
            val place = listOf(live.here.city, live.here.province, live.here.country).filter { it.isNotBlank() }
            if (place.isNotEmpty()) Text(place.joinToString("  ·  "), color = MeruAmber, style = MaterialTheme.typography.titleMedium)
            RouteMap(live.samples, live.marks, Modifier.fillMaxWidth().height(240.dp))
            Text("Speed", style = MaterialTheme.typography.titleMedium)
            LineChart(live.samples.map { it.speedKmh.toFloat() }, MeruAmber)
            Text("Forces", style = MaterialTheme.typography.titleMedium)
            LineChart(live.samples.map { it.longG.toFloat() }, MeruPaper)
            Text("The road wrote this", style = MaterialTheme.typography.titleMedium)
            if (live.marks.isEmpty()) Text("Brakes, lane changes, and the climb appear as the trace builds.", color = MeruMist)
            live.marks.takeLast(8).asReversed().forEach { mark ->
                Text(mark.note, color = if (mark.kind == "brake") MeruBrake else MeruPaper, style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val car = viewModel.profile()
            Text(car?.carName ?: "Your car", style = MaterialTheme.typography.displayLarge, color = MeruPaper)
            Text(viewModel.windowLine(), color = MeruPaper, style = MaterialTheme.typography.titleLarge)
            Text("${viewModel.points()} points", color = MeruAmber, style = MaterialTheme.typography.displayMedium.copy(fontSize = 40.sp))
            if (!platformHasGps()) {
                Text(
                    "This machine has no GPS, so Start traces a moving drive you can finish. On a phone, the same button follows the road and keeps going.",
                    color = MeruMist,
                )
            }
            checkpoint?.let { open ->
                Text("A drive was still running.", style = MaterialTheme.typography.titleMedium)
                Text("${km(open.distanceM)}  ${open.points.size} points", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = LocalMeruMono.current))
                MeruButton("Keep it") { onFinished(viewModel.keepCheckpoint()) }
                MeruButton("Discard", primary = false) { viewModel.discardCheckpoint() }
            }
            note?.let { Text(it, color = MeruAmber) }
        }
        RideControls(
            running = live.active,
            onStart = {
                if (checkpoint != null) onNote("Keep or discard the drive that was still running before starting another.")
                else viewModel.start()
            },
            onStop = { onFinished(viewModel.stop()) },
        )
    }
}

@Composable
private fun LogTab(drives: List<SavedDrive>, onOpen: (String) -> Unit) {
    MeruColumn {
        Text("The log", style = MaterialTheme.typography.headlineMedium)
        if (drives.isEmpty()) {
            Text("No drives yet. Start one and the page writes itself.", color = MeruMist, style = MaterialTheme.typography.bodyLarge)
        }
        drives.forEach { drive ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xC0141C18))
                    .border(1.dp, Color(0x33E8A317), RoundedCornerShape(20.dp))
                    .clickable { onOpen(drive.id) }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(drive.storyTitle.ifBlank { clock(drive.startedAt) }, style = MaterialTheme.typography.titleLarge, color = MeruPaper)
                Text(
                    "${km(drive.distanceM)}    ${drive.score} pts",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalMeruMono.current),
                    color = MeruAmber,
                )
                Text("${drive.city.ifBlank { "Unplaced" }}   ${drive.brakeCount} brakes   ${drive.weather}", color = MeruMist)
            }
        }
    }
}

@Composable
private fun MissionHome(onMissions: () -> Unit, onExpeditions: () -> Unit, onRelics: () -> Unit) {
    MeruColumn {
        Text("Missions", style = MaterialTheme.typography.displayLarge, color = MeruPaper)
        Text("Sixteen roads. Claimed only by a real drive.", color = MeruMist, style = MaterialTheme.typography.titleMedium)
        MeruPlate("The atlas", "The sixteen named roads.", onMissions)
        MeruPlate("Expeditions", "Chapters that close when the road does.", onExpeditions)
        MeruPlate("Relics", "What you stood beside.", onRelics)
    }
}

@Composable
private fun BoardsTab(viewModel: MeruViewModel) {
    var tier by remember { mutableStateOf("city") }
    val rows = remember(tier, viewModel.revision.collectAsStateWithLifecycle().value) { viewModel.board(tier) }
    MeruColumn {
        Text("Boards", style = MaterialTheme.typography.displayLarge, color = MeruPaper)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("city" to "City", "province" to "Province", "country" to "Country", "world" to "World").forEach { (id, label) ->
                Text(
                    label,
                    color = if (tier == id) Color(0xFF1A1406) else MeruPaper,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (tier == id) MeruAmber else Color(0xCC141C18))
                        .clickable { tier = id }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        rows.forEach { row ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (row.you) Color(0x33E8A317) else Color(0xB3121916))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${row.rank}   ${row.name}", color = MeruPaper, style = MaterialTheme.typography.titleMedium)
                Text(row.points.toString(), color = MeruAmber, style = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalMeruMono.current))
            }
        }
    }
}

@Composable
private fun YouTab(
    viewModel: MeruViewModel,
    onChronicle: () -> Unit,
    onConvoy: () -> Unit,
    onContracts: () -> Unit,
    onStory: () -> Unit,
    onSignOut: () -> Unit,
) {
    val profile = viewModel.profile()
    MeruColumn {
        Text(profile?.name ?: "Driver", style = MaterialTheme.typography.displayLarge, color = MeruPaper)
        Text("${profile?.carName ?: "Car"}   ${profile?.make.orEmpty()} ${profile?.model.orEmpty()}".trim(), color = MeruAmber, style = MaterialTheme.typography.titleLarge)
        Text("${profile?.city.orEmpty()}   ${profile?.province.orEmpty()}   ${profile?.country.orEmpty()}", color = MeruMist)
        Text(viewModel.points().toString(), style = MaterialTheme.typography.displayMedium, color = MeruAmber)
        Text("points on the car", color = MeruMist)
        Text("Joining puts 100 on the car. MERU adds 80. Distance, a clean trace, and climb add the rest.", color = MeruMist, style = MaterialTheme.typography.bodyLarge)
        Text("Your code  ${profile?.referralCode ?: "—"}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalMeruMono.current))
        MeruPlate("Chronicle", "Verified kilometres, roads, relics, and the bond.", onChronicle)
        MeruPlate("Convoy", "Up to eight. Shared kilometres only while you are in.", onConvoy)
        MeruPlate("Rituals and contracts", "Six windows, and this week's eight contracts.", onContracts)
        MeruPlate("Story", "The latest page written from a real trace.", onStory)
        MeruButton("Sign out", primary = false, onClick = onSignOut)
    }
}

@Composable
fun SummaryScreen(driveId: String, onClose: () -> Unit) {
    val viewModel = koinViewModel<MeruViewModel>()
    val drive = viewModel.drive(driveId)
    if (drive == null) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("That drive is not in the log.", style = MaterialTheme.typography.titleLarge)
            MeruButton("Back", primary = false, onClick = onClose)
        }
        return
    }
    val score = remember { Animatable(0f) }
    LaunchedEffect(drive.id) { score.animateTo(drive.score.toFloat(), tween(1200)) }
    MeruColumn {
        Text(drive.storyTitle, style = MaterialTheme.typography.headlineMedium)
        Text("${score.value.toInt()}", style = MaterialTheme.typography.displayMedium, color = MeruAmber)
        Text("points from this drive", color = MeruMist)
        Text(drive.storyBody, style = MaterialTheme.typography.bodyLarge)
        RouteMap(drive.points, drive.marks, Modifier.fillMaxWidth().height(240.dp))
        StatRow("Distance", km(drive.distanceM), "Time", duration(drive.startedAt, drive.endedAt))
        StatRow("Average", "${drive.avgSpeedKmh.toInt()} km/h", "Peak", "${drive.maxSpeedKmh.toInt()} km/h")
        StatRow("Peak G", one(drive.peakG), "Climbed", "${drive.elevationGainM.toInt()} m")
        StatRow("Brakes", drive.brakeCount.toString(), "Lane changes", drive.laneChanges.toString())
        Text("${drive.weather.ifBlank { "Sky unread" }} · ${drive.tempC?.let { "${it.toInt()}°" } ?: "—"} · AQI ${drive.aqi ?: "—"} · ${listOf(drive.city, drive.province, drive.country).filter { it.isNotBlank() }.joinToString(" · ")}")
        Text("Speed", style = MaterialTheme.typography.titleMedium)
        LineChart(drive.points.map { it.speedKmh.toFloat() }, MeruAmber)
        Text("Forces", style = MaterialTheme.typography.titleMedium)
        LineChart(drive.points.map { it.longG.toFloat() }, MeruPaper)
        drive.marks.forEach { mark ->
            Text("${mark.note} · ${clock(mark.t)}", color = if (mark.kind == "brake") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary)
        }
        if (drive.points.size < 2) {
            Text("This trace has fewer than two points, so it cannot claim a road or a relic.", color = MaterialTheme.colorScheme.error)
        }
        MeruButton("Back to the car", onClick = onClose)
    }
}

@Composable
fun RouteMap(samples: List<RideSample>, marks: List<RideMark>, modifier: Modifier) {
    val ink = MeruAmber
    val brake = MeruBrake
    val lane = MeruPaper
    if (samples.isEmpty()) {
        Text("The map fills as you move.", modifier = modifier.padding(12.dp), color = MeruMist)
        return
    }
    Canvas(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xCC101714))
            .border(1.dp, Color(0x55E8A317), RoundedCornerShape(22.dp)),
    ) {
        val minLat = samples.minOf { it.lat }
        val maxLat = samples.maxOf { it.lat }
        val minLon = samples.minOf { it.lon }
        val maxLon = samples.maxOf { it.lon }
        val latSpan = (maxLat - minLat).takeIf { it > 0.00001 } ?: 0.001
        val lonSpan = (maxLon - minLon).takeIf { it > 0.00001 } ?: 0.001
        fun point(lat: Double, lon: Double): Offset {
            val x = ((lon - minLon) / lonSpan).toFloat() * size.width * 0.86f + size.width * 0.07f
            val y = (1f - ((lat - minLat) / latSpan).toFloat()) * size.height * 0.86f + size.height * 0.07f
            return Offset(x, y)
        }
        val path = Path()
        samples.forEachIndexed { index, sample ->
            val p = point(sample.lat, sample.lon)
            if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        drawPath(path, ink, style = Stroke(width = 6f, cap = StrokeCap.Round))
        marks.forEach { mark ->
            drawCircle(if (mark.kind == "brake") brake else lane, radius = 7f, center = point(mark.lat, mark.lon))
        }
    }
}

@Composable
private fun LineChart(values: List<Float>, color: Color) {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xCC101714))
            .border(1.dp, Color(0x33E8A317), RoundedCornerShape(18.dp)),
    ) {
        if (values.size < 2) return@Canvas
        val min = values.min()
        val max = values.max().takeIf { it > min } ?: (min + 1f)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index / (values.size - 1f) * size.width
            val y = size.height - ((value - min) / (max - min)) * size.height * 0.8f - size.height * 0.1f
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun StatRow(left: String, leftValue: String, right: String, rightValue: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column { Text(left, color = MeruMist, style = MaterialTheme.typography.labelMedium); Text(leftValue, style = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalMeruMono.current)) }
        Column { Text(right, color = MeruMist, style = MaterialTheme.typography.labelMedium); Text(rightValue, style = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalMeruMono.current)) }
    }
}

private fun km(metres: Double): String = "${(metres / 1000.0).let { (it * 10).toInt() / 10.0 }} km"

private fun one(value: Double): String {
    val tenths = (value * 10).toInt() / 10.0
    return tenths.toString()
}

private fun g(long: Double, lat: Double): String {
    val value = kotlin.math.sqrt(long * long + lat * lat)
    return one(value)
}

private fun clock(ms: Long): String {
    if (ms <= 0L) return "—"
    val pkt = ms + 5 * 3_600_000
    val seconds = ((pkt / 1000) % 86_400).toInt()
    val hour = (seconds / 3600).toString().padStart(2, '0')
    val minute = ((seconds % 3600) / 60).toString().padStart(2, '0')
    return "$hour:$minute"
}

private fun elapsed(started: Long): String {
    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    return duration(started, now)
}

private fun duration(started: Long, ended: Long): String {
    val seconds = ((ended - started) / 1000).coerceAtLeast(0)
    val minute = seconds / 60
    val second = (seconds % 60).toString().padStart(2, '0')
    return "$minute:$second"
}
