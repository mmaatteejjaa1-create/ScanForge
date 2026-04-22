package com.scanforge.ui.screens

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.accompanist.permissions.*
import com.scanforge.Screen
import com.scanforge.ui.components.*
import com.scanforge.ui.theme.ScanColors
import com.scanforge.ui.theme.ScanTypography
import com.scanforge.viewmodel.AiDepthViewModel

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun AiDepthScreen(
    navController: NavController,
    vm: AiDepthViewModel = hiltViewModel()
) {
    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    val inferenceTime by vm.inferenceTimeMs.collectAsState()
    val confidence by vm.confidence.collectAsState()
    val isProcessing by vm.isProcessing.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        cameraPermission.launchPermissionRequest()
        vm.startDepthStream(context, lifecycleOwner)
    }

    // Pulse animation for AI badge
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOut), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Camera preview (base layer)
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
                modifier = Modifier.fillMaxWidth().height(440.dp),
                update = { preview -> vm.bindCamera(context, lifecycleOwner, preview) }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(440.dp).background(ScanColors.Bg2),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = ScanColors.Text3, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    SecondaryButton("Dozvoli kameru", onClick = { cameraPermission.launchPermissionRequest() })
                }
            }
        }

        // ── Depth color overlay (heatmap)
        DepthHeatmapOverlay(
            modifier = Modifier.fillMaxWidth().height(440.dp)
        )

        // ── Depth bar (right side legend)
        Box(modifier = Modifier.fillMaxWidth().height(440.dp)) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .width(20.dp)
                    .height(160.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFEF4444), Color(0xFFF97316), Color(0xFF22D3A5), Color(0xFF6366F1))
                        )
                    )
            ) {}
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 36.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Text("blizu", style = ScanTypography.BodyS, color = Color.White.copy(0.6f))
                Text("1m", style = ScanTypography.BodyS, color = Color.White.copy(0.6f))
                Text("2m", style = ScanTypography.BodyS, color = Color.White.copy(0.6f))
                Text("daleko", style = ScanTypography.BodyS, color = Color.White.copy(0.6f))
            }
        }

        // ── Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // AI badge
            Row(
                modifier = Modifier
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(20.dp))
                    .border(1.dp, ScanColors.Accent.copy(0.4f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(ScanColors.Green.copy(pulseAlpha), androidx.compose.foundation.shape.CircleShape)
                )
                Text("MiDaS AI · Live", style = ScanTypography.BodyS, color = ScanColors.AccentPale, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
            }

            Text("${inferenceTime}ms", style = ScanTypography.Mono, color = ScanColors.Text2)
        }

        // ── Capture button (center bottom of camera view)
        Box(
            modifier = Modifier.fillMaxWidth().height(440.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .size(80.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(Brush.radialGradient(ScanColors.GradientWarm))
                    .border(3.dp, Color.White.copy(0.3f), androidx.compose.foundation.shape.CircleShape)
                    .clickable(enabled = !isProcessing) {
                        vm.captureDepthFrame(context) { modelPath ->
                            navController.navigate(Screen.Processing.createRoute("depth"))
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
            }
        }

        // ── Bottom panel
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(ScanColors.Bg)
                .padding(20.dp)
        ) {
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile("512px", "Rezolucija", modifier = Modifier.weight(1f))
                MetricTile("${inferenceTime}ms", "Inference", modifier = Modifier.weight(1f), valueColor = ScanColors.Green)
                MetricTile("${confidence}%", "Tačnost", modifier = Modifier.weight(1f), valueColor = ScanColors.Orange)
            }
            Spacer(Modifier.height(16.dp))

            // Info note
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScanColors.Accent.copy(0.08f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = ScanColors.AccentLight, modifier = Modifier.size(18.dp))
                Text(
                    "AI procenjuje dubinu iz jednog kadra. Za veću preciznost, koristi Photogrammetry mod.",
                    style = ScanTypography.BodyS,
                    color = ScanColors.Text2
                )
            }
        }
    }
}

// ─── Depth Heatmap Overlay ────────────────────────────────────────────────────
@Composable
private fun DepthHeatmapOverlay(modifier: Modifier = Modifier) {
    // Simulated heatmap overlay — in production this overlays the TFLite output bitmap
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color(0xFF6366F1).copy(0.05f),
                        Color(0xFF22D3A5).copy(0.10f),
                        Color(0xFFF97316).copy(0.18f),
                        Color(0xFFEF4444).copy(0.25f)
                    )
                )
            )
    )
}
