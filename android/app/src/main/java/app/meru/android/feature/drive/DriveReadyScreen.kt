package app.meru.android.feature.drive

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import java.util.Locale
import java.util.concurrent.TimeUnit

private val modes = listOf("Minimal", "Detailed", "Performance", "HUD")

@Composable
fun DriveReadyScreen(
    viewModel: DriveViewModel = hiltViewModel(),
) {
    val telemetry by viewModel.telemetry.collectAsState()
    val ui by viewModel.ui.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshPermissions()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    if (ui.confirmEnd) {
        AlertDialog(
            onDismissRequest = viewModel::dismissEndConfirm,
            title = { Text("End drive?") },
            text = { Text("Telemetry will stop and the trip will be saved on this device.") },
            confirmButton = {
                TextButton(onClick = viewModel::endDrive) { Text("End drive") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEndConfirm) { Text("Keep driving") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("DRIVE", color = MeruTeal, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier = Modifier.height(8.dp))
            Text(
                if (telemetry.active) "Ascent in progress" else "Ready when you are",
                color = MeruText,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier = Modifier.height(8.dp))
            Text(
                "Meru records GPS in a foreground service — keep going with the screen off.",
                color = MeruMuted,
            )

            if (ui.needsLocationPermission || ui.needsNotificationPermission) {
                Spacer(Modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MeruElevated)
                        .padding(16.dp),
                ) {
                    Text("Permissions", color = MeruAmber, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier = Modifier.height(6.dp))
                    Text(
                        "Location powers live speed and route. Notifications keep the drive service visible.",
                        color = MeruMuted,
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier = Modifier.height(12.dp))
                    MeruSecondaryButton(
                        text = "Grant permissions",
                        onClick = {
                            val perms = buildList {
                                add(Manifest.permission.ACCESS_FINE_LOCATION)
                                add(Manifest.permission.ACCESS_COARSE_LOCATION)
                                if (Build.VERSION.SDK_INT >= 33) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }.toTypedArray()
                            permissionLauncher.launch(perms)
                        },
                    )
                }
            }

            Spacer(Modifier = Modifier.height(16.dp))
            Text("Dashboard mode", color = MeruMuted)
            Spacer(Modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                modes.forEach { item ->
                    FilterChip(
                        selected = ui.mode == item,
                        onClick = { viewModel.setMode(item) },
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

            Spacer(Modifier = Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MeruElevated)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = telemetry.speedKmh.toInt().toString(),
                    color = MeruText,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text("km/h", color = MeruTeal, fontSize = 16.sp, letterSpacing = 2.sp)
                if (ui.mode != "Minimal") {
                    Spacer(Modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Metric("AVG", String.format(Locale.US, "%.0f", telemetry.avgSpeedKmh))
                        Metric("MAX", String.format(Locale.US, "%.0f", telemetry.maxSpeedKmh))
                        Metric("KM", String.format(Locale.US, "%.2f", telemetry.distanceM / 1000.0))
                    }
                    Spacer(Modifier = Modifier.height(8.dp))
                    Text(formatDuration(telemetry.durationMs), color = MeruMuted)
                }
                if (telemetry.gpsWeak) {
                    Spacer(Modifier = Modifier.height(8.dp))
                    Text("Weak GPS", color = MeruAmber)
                }
                if (telemetry.rejectedJumps > 0) {
                    Text("Filtered jumps: ${telemetry.rejectedJumps}", color = MeruMuted, fontSize = 12.sp)
                }
            }

            ui.error?.let {
                Spacer(Modifier = Modifier.height(12.dp))
                Text(it, color = MeruDanger)
            }
            ui.lastCompleted?.let { trip ->
                Spacer(Modifier = Modifier.height(12.dp))
                Text(
                    "Saved trip ${trip.id.take(8)} · ${"%.2f".format(trip.distanceM / 1000)} km · ${trip.pointCount} pts",
                    color = MeruTeal,
                    fontSize = 13.sp,
                )
            }
        }

        if (telemetry.active) {
            MeruPrimaryButton(text = "End drive", onClick = viewModel::requestEndConfirm)
        } else {
            MeruPrimaryButton(
                text = "Start drive",
                onClick = viewModel::startDrive,
                enabled = !ui.needsLocationPermission,
            )
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

private fun formatDuration(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

