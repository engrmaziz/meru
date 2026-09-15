package app.meru.android.feature.drive

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import app.meru.android.core.designsystem.components.MeruPrimaryButton
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruAmber
import app.meru.android.core.designsystem.theme.MeruDanger
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import app.meru.android.engine.sensors.MotionSample
import app.meru.android.engine.telemetry.LiveTelemetry
import java.util.Locale
import java.util.concurrent.TimeUnit

private val modes = listOf("Minimal", "Detailed", "Performance", "HUD")

@Composable
fun DriveReadyScreen(
    viewModel: DriveViewModel = hiltViewModel(),
    onOpenCalibration: () -> Unit = {},
    onTripEnded: (String) -> Unit = {},
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val motion by viewModel.motion.collectAsState()
    val route by viewModel.route.collectAsState()
    val ui by viewModel.ui.collectAsState()
    val a11y = LocalAccessibilityManager.current
    val reduceMotion = a11y?.isEnabled == true
    val enterScale by animateFloatAsState(
        targetValue = 1f,
        label = "driveEnter",
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshPermissions() }

    LaunchedEffect(Unit) { viewModel.refreshPermissions() }
    LaunchedEffect(Unit) {
        viewModel.openProcessing.collect { tripId -> onTripEnded(tripId) }
    }

    if (ui.confirmEnd) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEndConfirm,
            title = { Text("End drive?") },
            text = { Text("Telemetry stops and the trip is saved on this device.") },
            confirmButton = {
                TextButton(onClick = viewModel::endDrive) { Text("End drive") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEndConfirm) { Text("Keep driving") }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .then(if (reduceMotion) Modifier else Modifier.graphicsLayer { scaleX = enterScale; scaleY = enterScale }),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("DRIVE", color = MeruTeal, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(
                if (telemetry.active) "Ascent in progress" else "Cockpit ready",
                color = MeruText,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            ui.activeVehicleLabel?.let {
                Text("Vehicle · $it", color = MeruMuted, fontSize = 13.sp)
            }

            if (ui.needsLocationPermission || ui.needsNotificationPermission) {
                PermissionCard {
                    val perms = buildList {
                        add(Manifest.permission.ACCESS_FINE_LOCATION)
                        add(Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }.toTypedArray()
                    permissionLauncher.launch(perms)
                }
            }

            if (!ui.calibrated) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MeruElevated)
                        .padding(14.dp),
                ) {
                    Text("Phone calibration", color = MeruAmber, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Mount the phone, then calibrate once for meaningful G readings.",
                        color = MeruMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MeruSecondaryButton(text = "Calibrate", onClick = onOpenCalibration)
                }
            }

            ModeRow(selected = ui.mode, onSelect = viewModel::setMode)

            SpeedHero(
                telemetry = telemetry,
                motion = motion,
                mode = ui.mode,
            )

            if (ui.mapExpanded) {
                DriveMap(
                    route = route,
                    latitude = telemetry.latitude,
                    longitude = telemetry.longitude,
                    bearing = telemetry.bearing,
                    expanded = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                )
            } else if (ui.mode != "HUD") {
                DriveMap(
                    route = route,
                    latitude = telemetry.latitude,
                    longitude = telemetry.longitude,
                    bearing = telemetry.bearing,
                )
            }

            MeruSecondaryButton(
                text = if (ui.mapExpanded) "Collapse map" else "Expand map",
                onClick = viewModel::toggleMapExpanded,
            )

            if (telemetry.gpsWeak) {
                OverlayBanner("Weak GPS — accuracy degraded", MeruAmber)
            }
            if (telemetry.permissionLost) {
                OverlayBanner("Location permission lost — open settings", MeruDanger)
            }
            ui.error?.let { OverlayBanner(it, MeruDanger) }

            Spacer(modifier = Modifier.height(8.dp))
            if (telemetry.active) {
                MeruPrimaryButton(
                    text = if (ui.ending) "Sealing…" else "End drive",
                    onClick = viewModel::requestEndConfirm,
                    enabled = !ui.ending,
                )
            } else {
                MeruPrimaryButton(
                    text = "Start drive",
                    onClick = viewModel::startDrive,
                    enabled = !ui.needsLocationPermission && !ui.ending,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionCard(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MeruElevated)
            .padding(16.dp),
    ) {
        Text("Permissions", color = MeruAmber, fontWeight = FontWeight.SemiBold)
        Text(
            "Location powers the live map and speed. Notifications keep the drive service visible.",
            color = MeruMuted,
            fontSize = 13.sp,
        )
        Spacer(modifier = Modifier.height(12.dp))
        MeruSecondaryButton(text = "Grant permissions", onClick = onGrant)
    }
}

@Composable
private fun ModeRow(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        modes.forEach { item ->
            FilterChip(
                selected = selected == item,
                onClick = { onSelect(item) },
                label = { Text(item, fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MeruTeal,
                    selectedLabelColor = MeruVoid,
                    containerColor = MeruElevated,
                    labelColor = MeruText,
                ),
            )
        }
    }
}

@Composable
private fun SpeedHero(
    telemetry: LiveTelemetry,
    motion: MotionSample,
    mode: String,
) {
    val hudMirror = mode == "HUD"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MeruElevated)
            .padding(24.dp)
            .graphicsLayer { if (hudMirror) scaleY = -1f },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = telemetry.speedKmh.toInt().toString(),
            color = MeruText,
            fontSize = if (mode == "Minimal" || mode == "HUD") 84.sp else 68.sp,
            fontWeight = FontWeight.Bold,
        )
        Text("km/h", color = MeruTeal, fontSize = 16.sp, letterSpacing = 2.sp)

        when (mode) {
            "Detailed", "Performance" -> {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Metric("AVG", String.format(Locale.US, "%.0f", telemetry.avgSpeedKmh))
                    Metric("MAX", String.format(Locale.US, "%.0f", telemetry.maxSpeedKmh))
                    Metric("KM", String.format(Locale.US, "%.2f", telemetry.distanceM / 1000.0))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(formatDuration(telemetry.durationMs), color = MeruMuted)
            }
        }

        if (mode == "Performance") {
            Spacer(modifier = Modifier.height(16.dp))
            GForceGauge(motion = motion)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Metric("ACC", String.format(Locale.US, "%+.2f", motion.longitudinalG))
                Metric(
                    "ALT",
                    telemetry.altitudeM?.let { String.format(Locale.US, "%.0fm", it) } ?: "—",
                )
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = MeruMuted, fontSize = 11.sp)
        Text(value, color = MeruText, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    }
}

@Composable
private fun OverlayBanner(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MeruElevated)
            .padding(12.dp),
    )
}

private fun formatDuration(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
