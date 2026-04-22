package com.scanforge.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.scanforge.Screen
import com.scanforge.ui.components.*
import com.scanforge.ui.theme.ScanColors
import com.scanforge.ui.theme.ScanTypography
import com.scanforge.viewmodel.PhotogrammetryViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotogrammetryScreen(
    navController: NavController,
    vm: PhotogrammetryViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val shotCount by vm.shotCount.collectAsState()
    val quality by vm.imageQuality.collectAsState()
    val angle by vm.currentAngle.collectAsState()
    val isCapturing by vm.isCapturing.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val targetShots = 30
    val progress = shotCount.toFloat() / targetShots

    LaunchedEffect(Unit) {
        cameraPermission.launchPermissionRequest()
    }

    // Flash animation when capturing
    var flashVisible by remember { mutableStateOf(false) }
    LaunchedEffect(shotCount) {
        if (shotCount > 0) {
            flashVisible = true
            kotlinx.coroutines.delay(80)
            flashVisible = false
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera preview
        if (cameraPermission.status.isGranted) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                },
                modifier = Modifier.fillMaxWidth().height(460.dp),
                update = { preview -> vm.bindCamera(context, lifecycleOwner, preview) }
            )
        } else {
            // Permission placeholder
            Box(
                modifier = Modifier.fillMaxWidth().height(460.dp).background(ScanColors.Bg2),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = ScanColors.Text3, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Dozvola za kameru je potrebna", color = ScanColors.Text2, style = ScanTypography.BodyM)
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Dozvoli kameru", onClick = { cameraPermission.launchPermissionRequest() })
                }
            }
        }

        // ── Scan frame overlay
        ScanFrameOverlay(
            modifier = Modifier.fillMaxWidth().height(460.dp),
            progress = progress
        )

        // ── Top info bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }

                // Shot counter
                Row(
                    modifier = Modifier
                        .background(Color.Black.copy(0.6f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "$shotCount",
                        style = ScanTypography.HeadingM,
                        color = ScanColors.AccentLight,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "/$targetShots snimaka",
                        style = ScanTypography.BodyM,
                        color = Color.White.copy(0.7f)
                    )
                }

                // Quality badge
                StatusChip(
                    label = "${quality}%",
                    color = when {
                        quality > 80 -> ScanColors.Green
                        quality > 60 -> ScanColors.Orange
                        else -> ScanColors.Red
                    }
                )
            }
        }

        // ── Flash effect
        AnimatedVisibility(
            visible = flashVisible,
            enter = fadeIn(tween(50)),
            exit = fadeOut(tween(80)),
            modifier = Modifier.fillMaxWidth().height(460.dp)
        ) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(0.4f)))
        }

        // ── Bottom controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .align(Alignment.Start),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(0.9f))
                            )
                        )
                        .padding(20.dp)
                ) {
                    // Progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NAPREDAK", style = ScanTypography.Label, color = Color.White.copy(0.5f))
                        Text("${(progress * 100).toInt()}%", style = ScanTypography.Label, color = ScanColors.AccentLight, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = ScanColors.Accent,
                        trackColor = Color.White.copy(0.15f)
                    )
                    Spacer(Modifier.height(16.dp))

                    // Capture row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Angle
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("UGAO", style = ScanTypography.Label, color = Color.White.copy(0.4f))
                            Text("${angle}°", style = ScanTypography.HeadingM, color = Color.White, fontFamily = FontFamily.Monospace)
                        }

                        // Shutter button
                        ShutterButton(
                            onClick = {
                                if (shotCount >= targetShots) {
                                    navController.navigate(Screen.Processing.createRoute("photogrammetry"))
                                } else {
                                    vm.capturePhoto(context)
                                }
                            },
                            active = !isCapturing
                        )

                        // Done/Process button area
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (shotCount >= 10) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            Brush.linearGradient(ScanColors.GradientSuccess),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            navController.navigate(Screen.Processing.createRoute("photogrammetry"))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Text("Procesiraj", style = ScanTypography.Label, color = ScanColors.Green)
                            } else {
                                Text("JOŠ", style = ScanTypography.Label, color = Color.White.copy(0.4f))
                                Text("${targetShots - shotCount}", style = ScanTypography.HeadingM, color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            // ── Guide panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScanColors.Bg)
                    .padding(20.dp)
            ) {
                SectionHeader("Vodič za skeniranje")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GuideStep("1", "Idi oko\nobjekata", modifier = Modifier.weight(1f))
                    GuideStep("2", "Drži 30–60cm\nrazdaljine", modifier = Modifier.weight(1f))
                    GuideStep("3", "Ravnomerno\nosvetljenje", modifier = Modifier.weight(1f))
                    GuideStep("4", "Bez refleksije\ni sjaja", modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ─── Scan Frame Overlay ───────────────────────────────────────────────────────
@Composable
fun ScanFrameOverlay(modifier: Modifier = Modifier, progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "scanY"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerSize = 40f
        val cx = w / 2
        val cy = h / 2
        val fx = cx - 180f
        val fy = cy - 180f
        val fw = 360f
        val fh = 360f
        val stroke = 2.5f
        val color = androidx.compose.ui.graphics.Color(0xFF6366F1)

        // Corner brackets
        listOf(
            Pair(fx, fy) to Pair(1f, 1f),
            Pair(fx + fw, fy) to Pair(-1f, 1f),
            Pair(fx, fy + fh) to Pair(1f, -1f),
            Pair(fx + fw, fy + fh) to Pair(-1f, -1f)
        ).forEach { (pos, dir) ->
            val (x, y) = pos
            val (dx, dy) = dir
            drawLine(color, Offset(x, y), Offset(x + dx * cornerSize, y), stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(x, y), Offset(x, y + dy * cornerSize), stroke, cap = StrokeCap.Round)
        }

        // Scan line
        val lineY = fy + fh * scanY
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(
                    androidx.compose.ui.graphics.Color.Transparent,
                    color,
                    androidx.compose.ui.graphics.Color.Transparent
                ),
                startX = fx, endX = fx + fw
            ),
            start = Offset(fx, lineY),
            end = Offset(fx + fw, lineY),
            strokeWidth = 2f
        )

        // Dot grid
        val dotSpacing = 22f
        val startX = fx + dotSpacing
        val startY = fy + dotSpacing
        var dotY = startY
        while (dotY < fy + fh) {
            var dotX = startX
            while (dotX < fx + fw) {
                drawCircle(
                    color = color.copy(alpha = 0.25f),
                    radius = 1.5f,
                    center = Offset(dotX, dotY)
                )
                dotX += dotSpacing
            }
            dotY += dotSpacing
        }
    }
}

// ─── Guide Step ───────────────────────────────────────────────────────────────
@Composable
private fun GuideStep(number: String, text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(ScanColors.Bg2, RoundedCornerShape(10.dp))
            .border(1.dp, ScanColors.Border, RoundedCornerShape(10.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(number, style = ScanTypography.HeadingM, color = ScanColors.AccentLight, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(4.dp))
        Text(text, style = ScanTypography.BodyS, color = ScanColors.Text2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
