package com.scanforge.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.scanforge.ui.theme.ScanColors
import com.scanforge.ui.theme.ScanTypography

// ─── Glass Card ───────────────────────────────────────────────────────────────
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val base = Modifier
        .background(ScanColors.Bg2, shape)
        .border(1.dp, ScanColors.Border, shape)
        .padding(16.dp)

    if (onClick != null) {
        Column(
            modifier = modifier
                .clip(shape)
                .clickable { onClick() }
                .then(base)
        ) { content() }
    } else {
        Column(modifier = modifier.then(base)) { content() }
    }
}

// ─── Primary Button ───────────────────────────────────────────────────────────
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradient: List<Color> = ScanColors.GradientPrimary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            tween(1200, easing = EaseInOut),
            RepeatMode.Reverse
        ), label = "glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.horizontalGradient(
                    if (enabled) gradient else listOf(ScanColors.Bg3, ScanColors.Bg3)
                )
            )
            .clickable(enabled = enabled && !isLoading) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = ScanTypography.BodyL.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) Color.White else ScanColors.Text3
            )
        }
    }
}

// ─── Secondary Button ─────────────────────────────────────────────────────────
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ScanColors.Bg2)
            .border(1.dp, ScanColors.BorderHigh, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        icon?.let {
            it()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = ScanTypography.BodyM.copy(fontWeight = FontWeight.Medium), color = ScanColors.Text1)
    }
}

// ─── Scan Progress Ring ───────────────────────────────────────────────────────
@Composable
fun ScanProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 6.dp
) {
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = EaseOut),
        label = "progress"
    )
    val color1 = ScanColors.Accent
    val color2 = ScanColors.Green

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val r = (size.toPx() / 2) - stroke

            // Background ring
            drawCircle(
                color = ScanColors.Border,
                radius = r,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
            )

            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(listOf(color1, color2, color1)),
                startAngle = -90f,
                sweepAngle = 360f * animProgress,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    stroke,
                    cap = StrokeCap.Round
                )
            )
        }

        // Center text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(animProgress * 100).toInt()}%",
                style = ScanTypography.HeadingM,
                color = ScanColors.Text1
            )
        }
    }
}

// ─── Status Chip / Tag ────────────────────────────────────────────────────────
@Composable
fun StatusChip(
    label: String,
    color: Color = ScanColors.Accent,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color.copy(alpha = 0.15f),
                RoundedCornerShape(20.dp)
            )
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Text(
            label.uppercase(),
            style = ScanTypography.Label,
            color = color
        )
    }
}

// ─── Pulsing Record Button ────────────────────────────────────────────────────
@Composable
fun ShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (active) 1.06f else 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(ScanColors.AccentLight, ScanColors.Accent)
                )
            )
            .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Capture",
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title.uppercase(),
        style = ScanTypography.Label,
        color = ScanColors.Text3,
        modifier = modifier
    )
}

// ─── Metric Tile ─────────────────────────────────────────────────────────────
@Composable
fun MetricTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    valueColor: Color = ScanColors.AccentLight
) {
    Column(
        modifier = modifier
            .background(ScanColors.Bg2, RoundedCornerShape(12.dp))
            .border(1.dp, ScanColors.Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = ScanTypography.HeadingM, color = valueColor)
        Spacer(Modifier.height(2.dp))
        Text(label, style = ScanTypography.BodyS, color = ScanColors.Text3)
    }
}
