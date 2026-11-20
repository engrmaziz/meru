package com.jetbrains.kmpapp.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kmp_app_template.shared.generated.resources.Res
import kmp_app_template.shared.generated.resources.jetbrainsmono_medium
import kmp_app_template.shared.generated.resources.outfit_bold
import kmp_app_template.shared.generated.resources.outfit_medium
import kmp_app_template.shared.generated.resources.outfit_regular
import kmp_app_template.shared.generated.resources.outfit_semibold
import org.jetbrains.compose.resources.Font

val MeruNight = Color(0xFF101412)
val MeruPanel = Color(0xFF1A211E)
val MeruLine = Color(0xFF31403A)
val MeruPaper = Color(0xFFF4EFE4)
val MeruMist = Color(0xFFD2DBD4)
val MeruAmber = Color(0xFFE8A317)
val MeruBrake = Color(0xFFFF8A7A)

private val EaseOut = CubicBezierEasing(0f, 0f, 0.2f, 1f)
private val Press = CubicBezierEasing(0.2f, 0f, 0f, 1f)

val LocalMeruMono = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

@Composable
expect fun meruAllowsMotion(): Boolean

@Composable
fun MeruTheme(content: @Composable () -> Unit) {
    val voice = FontFamily(
        Font(Res.font.outfit_regular, FontWeight.Normal),
        Font(Res.font.outfit_medium, FontWeight.Medium),
        Font(Res.font.outfit_semibold, FontWeight.SemiBold),
        Font(Res.font.outfit_bold, FontWeight.Bold),
    )
    val mono = FontFamily(Font(Res.font.jetbrainsmono_medium, FontWeight.Medium))
    val type = androidx.compose.material3.Typography(
        displayLarge = TextStyle(fontFamily = voice, fontWeight = FontWeight.Bold, fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = (-0.8).sp),
        displayMedium = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 64.sp, lineHeight = 64.sp),
        headlineMedium = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 32.sp, letterSpacing = (-0.4).sp),
        titleLarge = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
        titleMedium = TextStyle(fontFamily = voice, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp),
        bodyLarge = TextStyle(fontFamily = voice, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = voice, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        labelLarge = TextStyle(fontFamily = voice, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = voice, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 16.sp),
    )
    val colors = darkColorScheme(
        primary = MeruAmber,
        onPrimary = Color(0xFF1A1406),
        primaryContainer = Color(0xFF3A2C10),
        onPrimaryContainer = MeruPaper,
        tertiary = MeruPaper,
        onTertiary = MeruNight,
        secondary = MeruPaper,
        onSecondary = MeruNight,
        background = MeruNight,
        onBackground = MeruPaper,
        surface = MeruPanel,
        onSurface = MeruPaper,
        surfaceVariant = MeruPanel,
        onSurfaceVariant = MeruMist,
        outline = MeruLine,
        error = MeruBrake,
        onError = Color(0xFF2A0C08),
    )
    CompositionLocalProvider(LocalMeruMono provides mono) {
        MaterialTheme(colorScheme = colors, typography = type, content = content)
    }
}

@Composable
fun MeruColumn(content: @Composable () -> Unit) {
    val motion = meruAllowsMotion()
    val reveal = remember { Animatable(if (motion) 0f else 1f) }
    LaunchedEffect(motion) {
        if (motion) reveal.animateTo(1f, tween(280, easing = EaseOut))
    }
    Box(Modifier.fillMaxSize()) {
        NightBackdrop(Modifier.fillMaxSize())
        Column(
            Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
                .graphicsLayer {
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * 12f
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = { content() },
        )
    }
}

@Composable
fun NightBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFF070A09))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x66E8A317), Color(0x00070A09)),
                center = Offset(size.width * 0.82f, size.height * 0.02f),
                radius = size.minDimension * 0.95f,
            ),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x332E6BFF), Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.2f),
                radius = size.minDimension * 0.7f,
            ),
        )
        val ridge = Path().apply {
            moveTo(0f, size.height * 0.34f)
            quadraticTo(size.width * 0.22f, size.height * 0.18f, size.width * 0.46f, size.height * 0.3f)
            quadraticTo(size.width * 0.72f, size.height * 0.44f, size.width, size.height * 0.22f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(ridge, Color(0xFF101714))
        val road = Path().apply {
            moveTo(size.width * 0.5f, size.height)
            quadraticTo(size.width * 0.72f, size.height * 0.72f, size.width * 0.48f, size.height * 0.46f)
        }
        drawPath(road, Color(0x88E8A317), style = Stroke(width = 3.5f, cap = StrokeCap.Round))
    }
}

@Composable
fun SpeedFace(speed: Int, caption: String) {
    Box(Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 14f, cap = StrokeCap.Round)
            val pad = 28f
            val arcSize = Size(size.width - pad * 2, size.height * 1.35f)
            drawArc(MeruLine, 200f, 140f, false, Offset(pad, pad), arcSize, style = stroke)
            drawArc(MeruAmber, 200f, 140f * (speed / 160f).coerceIn(0f, 1f), false, Offset(pad, pad), arcSize, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$speed", style = MaterialTheme.typography.displayMedium, color = MeruPaper)
            Text(caption, color = MeruAmber, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun Instrument(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xB3121916))
            .border(1.dp, Color(0x33E8A317), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = MeruMist, style = MaterialTheme.typography.labelMedium)
        Text(value, color = MeruPaper, style = MaterialTheme.typography.titleLarge.copy(fontFamily = LocalMeruMono.current))
    }
}

@Composable
fun MeruButton(label: String, primary: Boolean = true, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && meruAllowsMotion()) 0.98f else 1f, tween(100, easing = Press))
    val background = if (primary) MeruAmber else MeruPanel
    val foreground = if (primary) Color(0xFF1A1406) else MeruPaper
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .then(if (primary) Modifier else Modifier.border(1.dp, MeruLine, RoundedCornerShape(16.dp)))
            .clickable(interaction, ripple(color = foreground.copy(alpha = 0.2f)), onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = foreground, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

@Composable
fun MeruPlate(title: String, line: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && meruAllowsMotion()) 0.98f else 1f, tween(100, easing = Press))
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xC0141C18))
            .border(1.dp, Color(0x44E8A317), RoundedCornerShape(22.dp))
            .clickable(interaction, ripple(color = MeruAmber.copy(alpha = 0.28f)), onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Canvas(Modifier.size(54.dp)) {
            val path = Path().apply {
                moveTo(size.width * 0.15f, size.height * 0.78f)
                quadraticTo(size.width * 0.4f, size.height * 0.2f, size.width * 0.85f, size.height * 0.35f)
            }
            drawPath(path, MeruAmber, style = Stroke(width = 3f, cap = StrokeCap.Round))
            drawCircle(MeruPaper, radius = 3.5f, center = Offset(size.width * 0.85f, size.height * 0.35f))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = MeruPaper, style = MaterialTheme.typography.titleLarge)
            Text(line, color = MeruMist, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun MeruField(label: String, value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MeruMist)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MeruAmber,
                unfocusedBorderColor = MeruLine,
                cursorColor = MeruAmber,
                focusedTextColor = MeruPaper,
                unfocusedTextColor = MeruPaper,
            ),
        )
    }
}

@Composable
fun MeruReading(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MeruMist, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = MeruPaper, style = MaterialTheme.typography.titleMedium.copy(fontFamily = LocalMeruMono.current))
    }
}

@Composable
fun MeruRule() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MeruLine))
}

@Composable
fun MeruNav(selected: String, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Color(0xE6101614))
            .border(1.dp, Color(0x33E8A317), RoundedCornerShape(28.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf("Drive", "Log", "Missions", "Boards", "You").forEach { name ->
            val on = name == selected
            val interaction = remember(name) { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed && meruAllowsMotion()) 0.98f else 1f, tween(100, easing = Press))
            Column(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (on) Color(0x33E8A317) else Color.Transparent)
                    .clickable(interaction, ripple(color = MeruAmber.copy(alpha = 0.3f))) { onSelect(name) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                MeruMark(name, if (on) MeruAmber else MeruMist)
                Text(name, color = if (on) MeruPaper else MeruMist, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun MeruMark(name: String, color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val stroke = Stroke(width = 1.75f, cap = StrokeCap.Round)
        when (name) {
            "Drive" -> {
                val path = Path().apply {
                    moveTo(size.width * 0.18f, size.height * 0.72f)
                    lineTo(size.width * 0.5f, size.height * 0.22f)
                    lineTo(size.width * 0.82f, size.height * 0.72f)
                }
                drawPath(path, color, style = stroke)
            }
            "Log" -> {
                repeat(3) { index ->
                    val y = size.height * (0.28f + index * 0.22f)
                    drawLine(color, Offset(size.width * 0.18f, y), Offset(size.width * 0.82f, y), strokeWidth = 1.75f, cap = StrokeCap.Round)
                }
            }
            "Missions" -> {
                val path = Path().apply {
                    moveTo(size.width * 0.12f, size.height * 0.74f)
                    lineTo(size.width * 0.38f, size.height * 0.42f)
                    lineTo(size.width * 0.58f, size.height * 0.58f)
                    lineTo(size.width * 0.88f, size.height * 0.2f)
                }
                drawPath(path, color, style = stroke)
            }
            "Boards" -> {
                val heights = listOf(0.45f, 0.72f, 0.32f)
                heights.forEachIndexed { index, height ->
                    val x = size.width * (0.22f + index * 0.28f)
                    drawLine(color, Offset(x, size.height * 0.8f), Offset(x, size.height * (0.8f - height)), strokeWidth = 1.75f, cap = StrokeCap.Round)
                }
            }
            else -> {
                drawCircle(color, radius = size.minDimension * 0.16f, center = Offset(size.width / 2f, size.height * 0.32f), style = stroke)
                drawArc(color, 200f, 140f, false, topLeft = Offset(size.width * 0.22f, size.height * 0.48f), size = androidx.compose.ui.geometry.Size(size.width * 0.56f, size.height * 0.42f), style = stroke)
            }
        }
    }
}
