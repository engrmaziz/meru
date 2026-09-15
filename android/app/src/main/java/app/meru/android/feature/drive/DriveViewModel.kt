package app.meru.android.feature.drive

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.database.VehicleDao
import app.meru.android.engine.drive.DriveForegroundService
import app.meru.android.engine.drive.DriveSessionController
import app.meru.android.engine.sensors.CalibrationStore
import app.meru.android.engine.sensors.MotionEngine
import app.meru.android.engine.sensors.MotionSample
import app.meru.android.engine.sync.TripSyncWorker
import app.meru.android.engine.telemetry.LiveTelemetry
import app.meru.android.engine.telemetry.RoutePoint
import app.meru.android.engine.trip.TripProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DriveUiState(
    val mode: String = "Detailed",
    val needsLocationPermission: Boolean = true,
    val needsNotificationPermission: Boolean = false,
    val calibrated: Boolean = false,
    val error: String? = null,
    val confirmEnd: Boolean = false,
    val mapExpanded: Boolean = false,
    val ending: Boolean = false,
    val activeVehicleLabel: String? = null,
)

@HiltViewModel
class DriveViewModel @Inject constructor(
    application: Application,
    private val session: DriveSessionController,
    private val tripProcessor: TripProcessor,
    private val vehicleDao: VehicleDao,
    motionEngine: MotionEngine,
    calibrationStore: CalibrationStore,
) : AndroidViewModel(application) {

    val telemetry: StateFlow<LiveTelemetry> = session.telemetry
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveTelemetry())

    val route: StateFlow<List<RoutePoint>> = session.route
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val motion: StateFlow<MotionSample> = motionEngine.motion
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MotionSample())

    private val _ui = MutableStateFlow(DriveUiState())
    val ui: StateFlow<DriveUiState> = _ui.asStateFlow()

    private val _openProcessing = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openProcessing: SharedFlow<String> = _openProcessing.asSharedFlow()

    init {
        refreshPermissions()
        viewModelScope.launch {
            calibrationStore.profile
                .map { it != null }
                .collect { calibrated ->
                    _ui.value = _ui.value.copy(calibrated = calibrated)
                }
        }
        viewModelScope.launch {
            val active = vehicleDao.active()
            _ui.value = _ui.value.copy(
                activeVehicleLabel = active?.let { "${it.nickname} · ${it.year}" },
            )
        }
    }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val notifNeeded = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        session.markPermissionLost(!fine && telemetry.value.active)
        _ui.value = _ui.value.copy(
            needsLocationPermission = !fine,
            needsNotificationPermission = notifNeeded,
        )
    }

    fun setMode(mode: String) {
        _ui.value = _ui.value.copy(mode = mode)
    }

    fun toggleMapExpanded() {
        _ui.value = _ui.value.copy(mapExpanded = !_ui.value.mapExpanded)
    }

    fun startDrive() {
        refreshPermissions()
        if (_ui.value.needsLocationPermission) {
            _ui.value = _ui.value.copy(error = "Location permission required to start a drive.")
            return
        }
        viewModelScope.launch {
            runCatching {
                val vehicleId = vehicleDao.active()?.id
                session.startDrive(vehicleId)
                DriveForegroundService.start(getApplication())
            }.onFailure { e ->
                _ui.value = _ui.value.copy(error = e.message ?: "Could not start drive")
            }
        }
    }

    fun requestEndConfirm() {
        _ui.value = _ui.value.copy(confirmEnd = true)
    }

    fun dismissEndConfirm() {
        _ui.value = _ui.value.copy(confirmEnd = false)
    }

    fun endDrive() {
        if (_ui.value.ending) return
        viewModelScope.launch {
            _ui.value = _ui.value.copy(confirmEnd = false, ending = true, error = null)
            runCatching {
                val completed = if (session.isActive) session.endDrive() else null
                getApplication<Application>().stopService(
                    android.content.Intent(getApplication(), DriveForegroundService::class.java),
                )
                if (completed != null) {
                    tripProcessor.process(completed.id)
                    TripSyncWorker.enqueue(getApplication())
                    _openProcessing.emit(completed.id)
                }
            }.onFailure { e ->
                _ui.value = _ui.value.copy(error = e.message ?: "Could not finish trip")
            }
            _ui.value = _ui.value.copy(ending = false)
        }
    }
}
