package app.meru.android.feature.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meru.android.core.designsystem.components.MeruPrimaryButton
import app.meru.android.core.designsystem.components.MeruSecondaryButton
import app.meru.android.core.designsystem.theme.MeruCyan
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.core.designsystem.theme.MeruVoid

@Composable
fun WelcomeScreen(
    onCreateAccount: () -> Unit,
    onSignIn: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MeruVoid, MeruVoid, MeruTeal.copy(alpha = 0.18f)),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "MERU",
                    color = MeruTeal,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                )
                Spacer(Modifier = Modifier.height(12.dp))
                Text(
                    text = "Every drive becomes an ascent.",
                    color = MeruText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier = Modifier.height(10.dp))
                Text(
                    text = "Live telemetry. Adventure ranks. A vault for every car.",
                    color = MeruMuted,
                    fontSize = 15.sp,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MeruTeal.copy(alpha = 0.05f),
                                    MeruCyan.copy(alpha = 0.25f),
                                    MeruTeal.copy(alpha = 0.05f),
                                ),
                            ),
                        ),
                )
                Spacer(Modifier = Modifier.height(28.dp))
                MeruPrimaryButton(text = "Create account", onClick = onCreateAccount)
                Spacer(Modifier = Modifier.height(12.dp))
                MeruSecondaryButton(text = "Sign in", onClick = onSignIn)
            }
        }
    }
}
