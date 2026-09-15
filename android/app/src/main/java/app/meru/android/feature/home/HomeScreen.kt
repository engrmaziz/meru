package app.meru.android.feature.home

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.meru.android.core.datastore.SessionStore
import app.meru.android.core.designsystem.theme.MeruElevated
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeViewModel @Inject constructor(
    sessionStore: SessionStore,
) : ViewModel() {
    val session = sessionStore.session.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        null,
    )
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsState()
    val name = session?.displayName ?: "Driver"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MeruVoid)
            .padding(20.dp),
    ) {
        Text("Good to see you,", color = MeruMuted, fontSize = 14.sp)
        Text(name, color = MeruText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MeruElevated)
                .padding(18.dp),
        ) {
            Text("LEVEL 1", color = MeruTeal, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.12f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MeruTeal,
                trackColor = MeruVoid,
            )
            Spacer(Modifier.height(8.dp))
            Text("1,200 / 10,000 XP", color = MeruMuted, fontSize = 13.sp)
        }

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatChip(modifier = Modifier.weight(1f), label = "Adventure", value = "—")
            StatChip(modifier = Modifier.weight(1f), label = "Rating", value = "—")
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatChip(modifier = Modifier.weight(1f), label = "City rank", value = "Soon")
            StatChip(modifier = Modifier.weight(1f), label = "Streak", value = "0")
        }

        Spacer(Modifier.height(24.dp))
        Text("Next ascent", color = MeruText, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Open Drive when you are ready. Phase 2 wires the engine.",
            color = MeruMuted,
        )
    }
}

@Composable
private fun StatChip(
    modifier: Modifier,
    label: String,
    value: String,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MeruElevated)
            .padding(14.dp),
    ) {
        Text(label, color = MeruMuted, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = MeruText, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    }
}
