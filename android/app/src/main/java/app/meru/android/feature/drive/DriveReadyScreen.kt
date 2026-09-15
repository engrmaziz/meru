package app.meru.android.feature.drive

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meru.android.core.designsystem.components.MeruPrimaryButton
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid

private val modes = listOf("Minimal", "Detailed", "Performance", "HUD")

@Composable
fun DriveReadyScreen() {
    var mode by remember { mutableStateOf("Detailed") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("DRIVE", color = MeruTeal, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(8.dp))
            Text("Ready when you are", color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Engine arrives in Phase 2. Pick a dashboard mode for the ascent.",
                color = MeruMuted,
            )
            Spacer(Modifier.height(20.dp))
            Text("Dashboard mode", color = MeruMuted)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                modes.forEach { item ->
                    FilterChip(
                        selected = mode == item,
                        onClick = { mode = item },
                        label = { Text(item) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MeruTeal,
                            selectedLabelColor = MeruVoid,
                            containerColor = MeruElevated,
                            labelColor = MeruText,
                        ),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MeruElevated)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("0", color = MeruText, fontSize = 72.sp, fontWeight = FontWeight.Bold)
                Text("km/h", color = MeruTeal, fontSize = 16.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text("Preview · $mode", color = MeruMuted)
            }
        }

        MeruPrimaryButton(
            text = "Start drive (Phase 2)",
            onClick = { /* Phase 2 */ },
            enabled = false,
        )
    }
}
