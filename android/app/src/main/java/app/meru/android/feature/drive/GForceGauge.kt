package app.meru.android.feature.drive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.meru.android.core.designsystem.theme.MeruMuted
import app.meru.android.core.designsystem.theme.MeruTeal
import app.meru.android.core.designsystem.theme.MeruText
import app.meru.android.engine.sensors.MotionSample
import kotlin.math.min

@Composable
fun GForceGauge(
    motion: MotionSample,
    modifier: Modifier = Modifier.size(120.dp),
) {
    val accent = MeruTeal
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("G-FORCE", color = MeruMuted, fontSize = 11.sp, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val r = min(size.width, size.height) / 2f
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = Color.White.copy(alpha = 0.08f), radius = r, center = c, style = Stroke(3f))
                drawCircle(color = Color.White.copy(alpha = 0.05f), radius = r * 0.55f, center = c, style = Stroke(2f))
                drawLine(Color.White.copy(0.2f), Offset(c.x - r, c.y), Offset(c.x + r, c.y), strokeWidth = 2f, cap = StrokeCap.Round)
                drawLine(Color.White.copy(0.2f), Offset(c.x, c.y - r), Offset(c.x, c.y + r), strokeWidth = 2f, cap = StrokeCap.Round)
                val scale = r * 0.85f
                val dx = (motion.lateralG.coerceIn(-1.2f, 1.2f) / 1.2f) * scale
                val dy = -(motion.longitudinalG.coerceIn(-1.2f, 1.2f) / 1.2f) * scale
                drawCircle(color = accent, radius = 10f, center = Offset(c.x + dx, c.y + dy))
            }
            Text(
                text = String.format("%.2f G", motion.magnitudeG),
                color = MeruText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
        if (!motion.calibrated) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Uncalibrated", color = MeruMuted, fontSize = 11.sp)
        }
    }
}
