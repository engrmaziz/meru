package app.meru.android.feature.drive

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.database.TripEntity
import app.meru.android.engine.drive.DriveForegroundService
import app.meru.android.engine.drive.DriveSessionController
import app.meru.android.engine.telemetry.LiveTelemetry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DriveUiState(
    val mode: String = "Detailed",
    val needsLocationPermission: Boolean = true,
    val needsNotificationPermission: Boolean = false,
    val error: String? = null,
    val lastCompleted: TripEntity? = null,
    val confirmEnd: Boolean = false,
)

@HiltViewModel
class DriveViewModel @Inject constructor(
    application: Application,
    private val session: DriveSessionController,
) : AndroidViewModel(application) {

    val telemetry: StateFlow<LiveTelemetry> = session.telemetry
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LiveTelemetry())

    private val _ui = MutableStateFlow(DriveUiState())
    val ui: StateFlow<DriveUiState> = _ui.asStateFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        val ctx = getApplication<Application>()
        val fine = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val notifNeeded = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        _ui.value = _ui.value.copy(
            needsLocationPermission = !fine,
            needsNotificationPermission = notifNeeded,
        )
    }

    fun setMode(mode: String) {
        _ui.value = _ui.value.copy(mode = mode)
    }

    fun startDrive() {
        refreshPermissions()
        if (_ui.value.needsLocationPermission) {
            _ui.value = _ui.value.copy(error = "Location permission required to start a drive.")
            return
        }
        viewModelScope.launch {
            runCatching {
                session.startDrive()
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
        viewModelScope.launch {
            val completed = if (session.isActive) session.endDrive() else null
            getApplication<Application>().stopService(
                android.content.Intent(getApplication(), DriveForegroundService::class.java),
            )
            _ui.value = _ui.value.copy(confirmEnd = false, lastCompleted = completed, error = null)
        }
    }

    fun clearError() {
        _ui.value = _ui.value.copy(error = null)
    }
}
