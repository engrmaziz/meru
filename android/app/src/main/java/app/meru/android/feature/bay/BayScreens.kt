package app.meru.android.feature.bay

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.database.VehicleDao
import app.meru.android.core.database.VehicleEntity
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
import app.meru.android.core.network.BookingDto
import app.meru.android.core.network.CreateBookingRequest
import app.meru.android.core.network.MeruApi
import app.meru.android.core.network.SlotDto
import app.meru.android.core.network.WorkshopDto
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private val fieldColors
    @Composable
    get() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MeruText,
        unfocusedTextColor = MeruText,
        focusedBorderColor = MeruTeal,
        unfocusedBorderColor = MeruMuted,
        cursorColor = MeruTeal,
        focusedLabelColor = MeruTeal,
        unfocusedLabelColor = MeruMuted,
    )

@HiltViewModel
class BayViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: MeruApi,
    vehicleDao: VehicleDao,
) : ViewModel() {
    val vehicles = vehicleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _workshops = MutableStateFlow<List<WorkshopDto>>(emptyList())
    val workshops: StateFlow<List<WorkshopDto>> = _workshops.asStateFlow()

    private val _bookings = MutableStateFlow<List<BookingDto>>(emptyList())
    val bookings: StateFlow<List<BookingDto>> = _bookings.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _makeFilter = MutableStateFlow("Audi")
    val makeFilter: StateFlow<String> = _makeFilter.asStateFlow()

    fun setMake(make: String) {
        _makeFilter.value = make
        loadWorkshops()
    }

    fun loadWorkshops(q: String? = null) {
        viewModelScope.launch {
            runCatching {
                _workshops.value = api.workshops(
                    make = _makeFilter.value.ifBlank { null },
                    lat = 31.5204,
                    lon = 74.3587,
                    q = q,
                    verifiedOnly = "true",
                ).items
                _error.value = null
            }.onFailure {
                _error.value = "Bay offline — retry when connected"
            }
        }
    }

    fun loadBookings() {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                _bookings.value = api.bookings("Bearer $token").items
            }.onFailure {
                _error.value = "Bookings offline"
            }
        }
    }

    fun cancelBooking(id: String) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            runCatching {
                api.cancelBooking("Bearer $token", id)
                loadBookings()
            }
        }
    }
}

@HiltViewModel
class WorkshopDetailViewModel @Inject constructor(
    private val sessionStore: SessionStore,
    private val api: MeruApi,
    vehicleDao: VehicleDao,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val workshopId: String = checkNotNull(savedStateHandle["workshopId"])

    val vehicles = vehicleDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _workshop = MutableStateFlow<WorkshopDto?>(null)
    val workshop: StateFlow<WorkshopDto?> = _workshop.asStateFlow()

    private val _slots = MutableStateFlow<List<SlotDto>>(emptyList())
    val slots: StateFlow<List<SlotDto>> = _slots.asStateFlow()

    private val _selectedServices = MutableStateFlow(listOf("oil_change"))
    val selectedServices: StateFlow<List<String>> = _selectedServices.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load() {
        viewModelScope.launch {
            runCatching {
                _workshop.value = api.workshop(workshopId)
                _slots.value = api.workshopSlots(workshopId).items
                _error.value = null
            }.onFailure {
                _error.value = "Workshop offline"
            }
        }
    }

    fun toggleService(id: String) {
        val cur = _selectedServices.value.toMutableList()
        if (cur.contains(id)) cur.remove(id) else cur.add(id)
        if (cur.isEmpty()) cur.add("oil_change")
        _selectedServices.value = cur
    }

    fun book(vehicle: VehicleEntity, slotId: String, onDone: (BookingDto) -> Unit) {
        viewModelScope.launch {
            val token = sessionStore.session.first().accessToken ?: return@launch
            _busy.value = true
            runCatching {
                val share = api.createHistoryShare("Bearer $token", vehicle.id)
                val booking = api.createBooking(
                    "Bearer $token",
                    CreateBookingRequest(
                        workshopId = workshopId,
                        vehicleId = vehicle.id,
                        slotId = slotId,
                        serviceIds = _selectedServices.value,
                        historyShareToken = share.token,
                    ),
                )
                _error.value = null
                onDone(booking)
            }.onFailure {
                _error.value = it.message?.take(80) ?: "Booking failed"
            }
            _busy.value = false
        }
    }
}

@Composable
fun WorkshopsScreen(
    onBack: () -> Unit,
    onOpenWorkshop: (String) -> Unit,
    onOpenBookings: () -> Unit,
    driving: Boolean = false,
    viewModel: BayViewModel = hiltViewModel(),
) {
    val workshops by viewModel.workshops.collectAsState()
    val make by viewModel.makeFilter.collectAsState()
    val error by viewModel.error.collectAsState()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.loadWorkshops() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "←",
                color = MeruTeal,
                fontSize = 22.sp,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 12.dp),
            )
            Text("Bay", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                if (driving) "Locked" else "My bookings",
                color = if (driving) MeruMuted else MeruCyan,
                fontSize = 14.sp,
                modifier = Modifier.clickable(enabled = !driving, onClick = onOpenBookings),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("Brand-fit workshops near Lahore.", color = MeruMuted)
        if (driving) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Driving Mode — booking locked", color = MeruAmber, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Audi", "Toyota", "Honda").forEach { brand ->
                FilterChip(
                    selected = make == brand,
                    onClick = { viewModel.setMake(brand) },
                    label = { Text(brand) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeruTeal.copy(alpha = 0.25f),
                        selectedLabelColor = MeruTeal,
                        labelColor = MeruMuted,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                viewModel.loadWorkshops(it.ifBlank { null })
            },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors,
            singleLine = true,
        )
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MeruAmber, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(workshops, key = { it.id }) { w ->
                WorkshopRow(w) {
                    if (!driving) onOpenWorkshop(w.id)
                }
            }
        }
    }
}

@Composable
private fun WorkshopRow(w: WorkshopDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MeruElevated)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(w.name, color = MeruText, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (w.brandFit >= 3) {
                Text("BEST FIT", color = MeruTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${w.kind} · ${w.brands.joinToString()} · ★ ${"%.1f".format(w.rating)} · ${w.distanceKm} km",
            color = MeruMuted,
            fontSize = 13.sp,
        )
        if (w.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(w.description, color = MeruMuted, fontSize = 12.sp, maxLines = 2)
        }
    }
}

@Composable
fun WorkshopDetailScreen(
    onBack: () -> Unit,
    onBooked: (BookingDto) -> Unit,
    driving: Boolean = false,
    viewModel: WorkshopDetailViewModel = hiltViewModel(),
) {
    val workshop by viewModel.workshop.collectAsState()
    val slots by viewModel.slots.collectAsState()
    val vehicles by viewModel.vehicles.collectAsState()
    val services by viewModel.selectedServices.collectAsState()
    val busy by viewModel.busy.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var selectedVehicle by remember { mutableStateOf<VehicleEntity?>(null) }
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(vehicles) {
        if (selectedVehicle == null) selectedVehicle = vehicles.firstOrNull()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            "← Back",
            color = MeruTeal,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(bottom = 12.dp),
        )
        val w = workshop
        if (w == null) {
            Text(error ?: "Loading…", color = MeruMuted)
            return
        }
        Text(w.name, color = MeruText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text("${w.kind} · ${w.brands.joinToString()} · ★ ${"%.1f".format(w.rating)}", color = MeruMuted)
        Spacer(modifier = Modifier.height(12.dp))
        WorkshopPinMap(lat = w.lat, lon = w.lon)
        Spacer(modifier = Modifier.height(12.dp))
        Text(w.description, color = MeruText, fontSize = 14.sp)
        w.hours?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Hours ${it.open}–${it.close} · ${w.bays} bays", color = MeruCyan, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Services", color = MeruText, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            w.services.take(4).forEach { s ->
                FilterChip(
                    selected = services.contains(s.id),
                    onClick = { viewModel.toggleService(s.id) },
                    label = { Text(s.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MeruTeal.copy(alpha = 0.25f),
                        selectedLabelColor = MeruTeal,
                        labelColor = MeruMuted,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Vehicle", color = MeruText, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        if (vehicles.isEmpty()) {
            Text("Add a car in Garage first.", color = MeruAmber, fontSize = 13.sp)
        } else {
            vehicles.forEach { v ->
                val selected = selectedVehicle?.id == v.id
                Text(
                    "${v.year} ${v.make} ${v.model}",
                    color = if (selected) MeruTeal else MeruText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) MeruTeal.copy(alpha = 0.12f) else MeruElevated)
                        .clickable { selectedVehicle = v }
                        .padding(12.dp),
                )
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Open slots", color = MeruText, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        slots.take(12).forEach { slot ->
            val selected = selectedSlot == slot.id
            Text(
                formatSlot(slot.startAtMs) + " · bay ${slot.bay}",
                color = if (selected) MeruTeal else MeruText,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (selected) MeruTeal else MeruMuted.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp),
                    )
                    .clickable { selectedSlot = slot.id }
                    .padding(12.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MeruAmber, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        MeruPrimaryButton(
            text = when {
                driving -> "Locked in Driving Mode"
                busy -> "Holding slot…"
                else -> "Confirm booking"
            },
            onClick = {
                val vehicle = selectedVehicle
                val slot = selectedSlot
                if (!driving && vehicle != null && slot != null && !busy) {
                    viewModel.book(vehicle, slot, onBooked)
                }
            },
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("History share attaches automatically for the workshop.", color = MeruMuted, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MyBookingsScreen(
    onBack: () -> Unit,
    driving: Boolean = false,
    viewModel: BayViewModel = hiltViewModel(),
) {
    val bookings by viewModel.bookings.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadBookings() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text(
            "← Back",
            color = MeruTeal,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(bottom = 12.dp),
        )
        Text("My bookings", color = MeruText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        if (driving) {
            Text("Driving Mode — cancel locked", color = MeruAmber, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(bookings, key = { it.id }) { b ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MeruElevated)
                        .padding(14.dp),
                ) {
                    Text(b.workshopName ?: b.workshopId, color = MeruText, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${b.statusLabel ?: b.status} · ${b.startAtMs?.let { formatSlot(it) } ?: "—"}",
                        color = MeruMuted,
                        fontSize = 13.sp,
                    )
                    if (b.historyShareToken != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("History shared", color = MeruCyan, fontSize = 12.sp)
                    }
                    if (!driving && b.status == "confirmed") {
                        Spacer(modifier = Modifier.height(10.dp))
                        MeruSecondaryButton(
                            text = "Cancel",
                            onClick = { viewModel.cancelBooking(b.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BookingConfirmedScreen(
    booking: BookingDto,
    onDone: () -> Unit,
) {
    val scale = remember { Animatable(0.7f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(450))
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
        ) {
            Text("BOOKED", color = MeruTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(booking.workshopName ?: "Workshop", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                booking.startAtMs?.let { formatSlot(it) } ?: "Confirmed",
                color = MeruCyan,
                fontSize = 16.sp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (booking.historyShareToken != null) "Timeline shared with the bay." else "Confirmed.",
                color = MeruMuted,
            )
            Spacer(modifier = Modifier.height(28.dp))
            MeruPrimaryButton(text = "Done", onClick = onDone)
        }
    }
}

@Composable
private fun WorkshopPinMap(lat: Double, lon: Double) {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).also { it.onCreate(null) }
    }
    DisposableEffect(Unit) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp)),
        factory = { mapView },
        update = { view ->
            view.getMapAsync { map ->
                if (map.style == null) {
                    map.setStyle(Style.Builder().fromUri("https://demotiles.maplibre.org/style.json")) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 13.0))
                    }
                } else {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lon), 13.0))
                }
            }
        },
    )
}

private fun formatSlot(ms: Long): String =
    SimpleDateFormat("EEE d MMM · HH:mm", Locale.getDefault()).format(Date(ms))
