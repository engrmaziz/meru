package app.meru.android.feature.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruVoid

@Composable
fun TripsStubScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Trips unlock after your first ascent.\n(Phase 4)",
            color = MeruMuted,
            fontSize = 16.sp,
        )
    }
}
