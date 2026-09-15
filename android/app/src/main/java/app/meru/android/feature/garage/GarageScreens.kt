package app.meru.android.feature.garage

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.database.TripDao
import app.meru.android.core.database.VehicleDao
import app.meru.android.core.database.VehicleDocumentEntity
import app.meru.android.core.database.VehicleEntity
import app.meru.android.core.database.VehicleServiceEntity
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
import app.meru.android.core.network.CreateServiceRequest
import app.meru.android.core.network.CreateVehicleRequest
import app.meru.android.core.network.MeruApi
import app.meru.android.core.network.PlayVerifyRequest
import app.meru.android.core.network.TimelineItemDto
import app.meru.android.core.network.TimelineResponse
import app.meru.android.engine.sync.TripSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class GarageViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: MeruApi,
    private val vehicleDao: VehicleDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    val vehicles = vehicleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _canAdd = MutableStateFlow(true)
    val canAdd: StateFlow<Boolean> = _canAdd.asStateFlow()

    private val _maxVehicles = MutableStateFlow(1)
    val maxVehicles: StateFlow<Int> = _maxVehicles.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                val res = api.vehicles("Bearer $token")
                _canAdd.value = res.canAdd
                _maxVehicles.value = res.maxVehicles
                vehicleDao.upsertAll(
                    res.vehicles.map {
                        VehicleEntity(
                            id = it.id,
                            make = it.make,
                            model = it.model,
                            year = it.year,
                            variant = it.variant,
                            powertrain = it.powertrain,
                            nickname = it.nickname,
                            vinMasked = it.vinMasked,
                            odometerKm = it.odometerKm,
                            active = it.active,
                        )
                    },
                )
                TripSyncWorker.enqueue(context)
                _error.value = null
            }.onFailure {
                _error.value = "Offline garage — showing local cars"
            }
        }
    }

    fun createVehicle(
        make: String,
        model: String,
        year: Int,
        nickname: String,
        onNeedPaywall: () -> Unit,
        onDone: (String) -> Unit,
    ) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                val v = api.createVehicle(
                    "Bearer $token",
                    CreateVehicleRequest(
                        make = make,
                        model = model,
                        year = year,
                        nickname = nickname.ifBlank { null },
                    ),
                )
                vehicleDao.upsert(
                    VehicleEntity(
                        id = v.id,
                        make = v.make,
                        model = v.model,
                        year = v.year,
                        nickname = v.nickname,
                        vinMasked = v.vinMasked,
                        odometerKm = v.odometerKm,
                        active = v.active,
                    ),
                )
                refresh()
                onDone(v.id)
            }.onFailure {
                if (it.message?.contains("403") == true || it.message?.contains("SLOT") == true) {
                    onNeedPaywall()
                } else {
                    // retrofit HttpException
                    val code = (it as? retrofit2.HttpException)?.code()
                    if (code == 403) onNeedPaywall()
                    else _error.value = it.message ?: "Could not add vehicle"
                }
            }
        }
    }

    fun purchaseSlot(onDone: () -> Unit) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                // ponytail: stub Play token until Billing Library wired
                api.verifyPlayPurchase(
                    "Bearer $token",
                    PlayVerifyRequest(purchaseToken = "dev-${UUID.randomUUID()}", sku = "meru_extra_vehicle_slot"),
                )
                refresh()
                onDone()
            }.onFailure {
                _error.value = "Purchase verify failed"
            }
        }
    }
}

@HiltViewModel
class VehicleDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionStore: SessionStore,
    private val api: MeruApi,
    private val vehicleDao: VehicleDao,
    private val tripDao: TripDao,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val vehicleId: String = checkNotNull(savedStateHandle["vehicleId"])

    private val _timeline = MutableStateFlow<TimelineResponse?>(null)
    val timeline: StateFlow<TimelineResponse?> = _timeline.asStateFlow()

    private val _merged = MutableStateFlow<List<TimelineItemDto>>(emptyList())
    val merged: StateFlow<List<TimelineItemDto>> = _merged.asStateFlow()

    private val _costTotal = MutableStateFlow(0.0)
    val costTotal: StateFlow<Double> = _costTotal.asStateFlow()

    private val _shareToken = MutableStateFlow<String?>(null)
    val shareToken: StateFlow<String?> = _shareToken.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                val tl = api.vehicleTimeline("Bearer $token", vehicleId)
                _timeline.value = tl
                val trips = tripDao.observeCompletedTrips().first()
                    .filter { it.vehicleId == vehicleId }
                    .map {
                        TimelineItemDto(
                            id = it.id,
                            atMs = it.startAtMs,
                            kind = "drive",
                            title = "Drive · ${"%.1f".format(it.distanceM / 1000)} km",
                            subtitle = "Q${it.qualityScore}",
                        )
                    }
                _merged.value = (tl.items + trips).sortedByDescending { it.atMs }
                _costTotal.value = api.vehicleCosts("Bearer $token", vehicleId).total
            }.onFailure {
                _message.value = "Timeline offline"
                val localServices = vehicleDao.servicesFor(vehicleId).map {
                    TimelineItemDto(
                        id = it.id,
                        atMs = it.atMs,
                        kind = "service",
                        title = it.workshopName ?: "Service",
                        subtitle = it.syncStatus,
                    )
                }
                _merged.value = localServices
            }
        }
    }

    fun addServiceOffline(
        workshop: String,
        labor: Double,
        parts: Double,
        odometer: Double,
        typeId: String,
    ) {
        viewModelScope.launch {
            val clientId = UUID.randomUUID().toString()
            val local = VehicleServiceEntity(
                id = clientId,
                vehicleId = vehicleId,
                clientServiceId = clientId,
                atMs = System.currentTimeMillis(),
                odometerKm = odometer,
                workshopName = workshop.ifBlank { "DIY / local shop" },
                serviceTypeIdsJson = "[\"$typeId\"]",
                laborCost = labor,
                partsCost = parts,
                nextDueAtMs = System.currentTimeMillis() + 90L * 86400000,
                syncStatus = "pending",
            )
            vehicleDao.upsertService(local)
            // Try immediate sync; else WorkManager
            val token = sessionStore.session.first().accessToken
            if (token != null) {
                runCatching {
                    api.createService(
                        "Bearer $token",
                        vehicleId,
                        CreateServiceRequest(
                            clientServiceId = clientId,
                            atMs = local.atMs,
                            odometerKm = odometer,
                            workshopName = local.workshopName,
                            serviceTypeIds = listOf(typeId),
                            laborCost = labor,
                            partsCost = parts,
                            nextDueAtMs = local.nextDueAtMs,
                        ),
                    )
                    vehicleDao.markService(clientId, "synced")
                }.onFailure {
                    TripSyncWorker.enqueue(context)
                }
            } else {
                TripSyncWorker.enqueue(context)
            }
            _message.value = "Service saved — syncing when online"
            load()
        }
    }

    fun addDocument(title: String, type: String) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                val doc = api.createDocument(
                    "Bearer $token",
                    vehicleId,
                    app.meru.android.core.network.DocumentCreateRequest(title = title, type = type),
                )
                // Simulate upload then confirm (signed URL stub)
                api.confirmDocument("Bearer $token", vehicleId, doc.id)
                vehicleDao.upsertDocument(
                    VehicleDocumentEntity(
                        id = doc.id,
                        vehicleId = vehicleId,
                        type = doc.type,
                        title = doc.title,
                        expiresAtMs = doc.expiresAtMs,
                        uploadUrl = doc.uploadUrl,
                        uploaded = true,
                    ),
                )
                _message.value = "Document uploaded"
                load()
            }.onFailure {
                _message.value = "Document upload failed"
            }
        }
    }

    fun createShare() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                val share = api.createHistoryShare("Bearer $token", vehicleId)
                _shareToken.value = share.token
            }
        }
    }
}

@Composable
fun GarageListScreen(
    onOpenVehicle: (String) -> Unit,
    onAddVehicle: () -> Unit,
    onPaywall: () -> Unit,
    viewModel: GarageViewModel = hiltViewModel(),
) {
    val vehicles by viewModel.vehicles.collectAsState()
    val canAdd by viewModel.canAdd.collectAsState()
    val max by viewModel.maxVehicles.collectAsState()
    val error by viewModel.error.collectAsState()
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text("Garage", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Text("Vault · $max slot${if (max == 1) "" else "s"}", color = MeruTeal, fontSize = 13.sp)
        error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MeruAmber, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        if (vehicles.isEmpty()) {
            Box(Modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Add your first car — one slot free.", color = MeruMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(vehicles, key = { it.id }) { v ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MeruElevated)
                            .clickable { onOpenVehicle(v.id) }
                            .padding(14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(v.nickname, color = MeruText, fontWeight = FontWeight.SemiBold)
                            if (v.active) Text("ACTIVE", color = MeruTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${v.year} ${v.make} ${v.model}", color = MeruMuted, fontSize = 13.sp)
                        Text(
                            String.format(Locale.US, "%.0f km", v.odometerKm),
                            color = MeruCyan,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        MeruPrimaryButton(
            text = if (canAdd) "Add vehicle" else "Unlock extra slot",
            onClick = { if (canAdd) onAddVehicle() else onPaywall() },
        )
    }
}

@Composable
fun AddVehicleScreen(
    onBack: () -> Unit,
    onNeedPaywall: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: GarageViewModel = hiltViewModel(),
) {
    var step by remember { mutableIntStateOf(0) }
    var make by remember { mutableStateOf("Toyota") }
    var model by remember { mutableStateOf("Corolla") }
    var year by remember { mutableStateOf("2018") }
    var nickname by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MeruText,
        unfocusedTextColor = MeruText,
        focusedBorderColor = MeruTeal,
        unfocusedBorderColor = MeruMuted,
        cursorColor = MeruTeal,
        focusedLabelColor = MeruTeal,
        unfocusedLabelColor = MeruMuted,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Add vehicle", color = MeruText, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("Step ${step + 1} of 3", color = MeruMuted)
        when (step) {
            0 -> {
                OutlinedTextField(make, { make = it }, label = { Text("Make") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(model, { model = it }, label = { Text("Model") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
            }
            1 -> {
                OutlinedTextField(year, { year = it }, label = { Text("Year") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(nickname, { nickname = it }, label = { Text("Nickname") }, colors = fieldColors, modifier = Modifier.fillMaxWidth())
            }
            else -> {
                Text("Confirm $year $make $model", color = MeruText, fontWeight = FontWeight.Medium)
                Text(nickname.ifBlank { "Will use make + model" }, color = MeruMuted)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (step < 2) {
            MeruPrimaryButton(text = "Continue", onClick = { step++ })
        } else {
            MeruPrimaryButton(
                text = "Save to vault",
                onClick = {
                    viewModel.createVehicle(
                        make = make,
                        model = model,
                        year = year.toIntOrNull() ?: 2020,
                        nickname = nickname,
                        onNeedPaywall = onNeedPaywall,
                        onDone = onCreated,
                    )
                },
            )
        }
        MeruSecondaryButton(text = "Back", onClick = {
            if (step == 0) onBack() else step--
        })
    }
}

@Composable
fun VehicleTimelineScreen(
    onBack: () -> Unit,
    onAddService: () -> Unit,
    viewModel: VehicleDetailViewModel = hiltViewModel(),
) {
    val timeline by viewModel.timeline.collectAsState()
    val merged by viewModel.merged.collectAsState()
    val cost by viewModel.costTotal.collectAsState()
    val share by viewModel.shareToken.collectAsState()
    val message by viewModel.message.collectAsState()
    var docTitle by remember { mutableStateOf("Insurance") }
    LaunchedEffect(Unit) { viewModel.load() }

    val v = timeline?.vehicle
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text(v?.nickname ?: "Vehicle", color = MeruText, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text(
            v?.let { "${it.year} ${it.make} ${it.model}" } ?: "",
            color = MeruMuted,
        )
        message?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MeruAmber, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryChip(Modifier.weight(1f), "Lifetime", String.format(Locale.US, "%.0f", cost))
            SummaryChip(Modifier.weight(1f), "Visits", "${timeline?.summary?.serviceVisits ?: 0}")
            SummaryChip(Modifier.weight(1f), "Docs", "${timeline?.summary?.documentCount ?: 0}")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(merged, key = { it.id + it.kind }) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MeruElevated)
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            item.kind.uppercase(Locale.US),
                            color = MeruTeal,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        if (item.meta?.certified == true) {
                            CertifiedStampChip()
                        }
                    }
                    Text(item.title, color = MeruText, fontWeight = FontWeight.Medium)
                    item.subtitle?.let { Text(it, color = MeruMuted, fontSize = 12.sp) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        MeruPrimaryButton(text = "Add service", onClick = onAddService)
        Spacer(Modifier.height(8.dp))
        MeruSecondaryButton(
            text = "Upload document",
            onClick = { viewModel.addDocument(docTitle, "insurance") },
        )
        Spacer(Modifier.height(8.dp))
        MeruSecondaryButton(text = "Create history share", onClick = { viewModel.createShare() })
        share?.let {
            Text("Share token: ${it.take(12)}…", color = MeruCyan, fontSize = 11.sp)
        }
        Spacer(Modifier.height(8.dp))
        MeruSecondaryButton(text = "Back", onClick = onBack)
    }
}

@Composable
fun AddServiceScreen(
    onBack: () -> Unit,
    viewModel: VehicleDetailViewModel = hiltViewModel(),
) {
    var workshop by remember { mutableStateOf("") }
    var labor by remember { mutableStateOf("2000") }
    var parts by remember { mutableStateOf("5000") }
    var odo by remember { mutableStateOf("45000") }
    val colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MeruText,
        unfocusedTextColor = MeruText,
        focusedBorderColor = MeruTeal,
        unfocusedBorderColor = MeruMuted,
        cursorColor = MeruTeal,
        focusedLabelColor = MeruTeal,
        unfocusedLabelColor = MeruMuted,
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Log service", color = MeruText, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("Works offline — queues to sync", color = MeruMuted, fontSize = 13.sp)
        OutlinedTextField(workshop, { workshop = it }, label = { Text("Workshop") }, colors = colors, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(odo, { odo = it }, label = { Text("Odometer km") }, colors = colors, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(labor, { labor = it }, label = { Text("Labor cost") }, colors = colors, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(parts, { parts = it }, label = { Text("Parts cost") }, colors = colors, modifier = Modifier.fillMaxWidth())
        MeruPrimaryButton(
            text = "Save",
            onClick = {
                viewModel.addServiceOffline(
                    workshop = workshop,
                    labor = labor.toDoubleOrNull() ?: 0.0,
                    parts = parts.toDoubleOrNull() ?: 0.0,
                    odometer = odo.toDoubleOrNull() ?: 0.0,
                    typeId = "oil_change",
                )
                onBack()
            },
        )
        MeruSecondaryButton(text = "Cancel", onClick = onBack)
    }
}

@Composable
fun VehiclePaywallScreen(
    onBack: () -> Unit,
    onPurchased: () -> Unit,
    viewModel: GarageViewModel = hiltViewModel(),
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Extra vault slot", color = MeruText, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Text(
            "One car is free. Unlock another slot to keep every story in Meru.",
            color = MeruMuted,
        )
        Spacer(Modifier.height(20.dp))
        MeruPrimaryButton(
            text = "Purchase slot (dev verify)",
            onClick = { viewModel.purchaseSlot(onPurchased) },
        )
        Spacer(Modifier.height(12.dp))
        MeruSecondaryButton(text = "Not now", onClick = onBack)
    }
}

@Composable
private fun SummaryChip(modifier: Modifier, label: String, value: String) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MeruElevated)
            .padding(10.dp),
    ) {
        Text(label, color = MeruMuted, fontSize = 11.sp)
        Text(value, color = MeruText, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CertifiedStampChip() {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(420))
    }
    Text(
        "CERTIFIED",
        color = MeruTeal,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                rotationZ = -8f
            }
            .border(1.dp, MeruTeal, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
