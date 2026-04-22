package com.scanforge.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.scanforge.Screen
import com.scanforge.ui.components.*
import com.scanforge.ui.theme.ScanColors
import com.scanforge.ui.theme.ScanTypography
import com.scanforge.viewmodel.ArViewerViewModel

@Composable
fun ArViewerScreen(
    navController: NavController,
    modelPath: String,
    vm: ArViewerViewModel = hiltViewModel()
) {
    val modelInfo by vm.modelInfo.collectAsState()
    val isArSupported by vm.isArSupported.collectAsState()
    val selectedTool by vm.selectedTool.collectAsState()

    LaunchedEffect(modelPath) { vm.loadModel(modelPath) }

    Box(modifier = Modifier.fillMaxSize().background(ScanColors.Bg)) {

        // ── AR View area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(460.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0A0F1E), Color(0xFF050810), Color(0xFF0D1020))
                    )
                )
        ) {
            // Grid floor perspective
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, ScanColors.Accent.copy(0.08f))
                        )
                    )
            )

            // 3D model placeholder / SceneView goes here
            // In production: ArSceneView or SceneView composable
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isArSupported) {
                    // ArSceneView integration point
                    ArSceneViewComposable(modelPath = modelPath)
                } else {
                    // Fallback 3D viewer (non-AR)
                    FallbackModelViewer(modelPath = modelPath)
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(
                        if (isArSupported) "AR aktivan" else "3D pregled",
                        if (isArSupported) ScanColors.Green else ScanColors.Orange
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButtonGlass(Icons.Default.Lightbulb) { vm.toggleLighting() }
                    IconButtonGlass(Icons.Default.ScreenShare) { /* share */ }
                }
            }

            // ── Transform tools (right side)
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransformToolButton("↔", "Pomeri", selectedTool == "move") { vm.selectTool("move") }
                TransformToolButton("↻", "Rotiraj", selectedTool == "rotate") { vm.selectTool("rotate") }
                TransformToolButton("⤢", "Skaliraj", selectedTool == "scale") { vm.selectTool("scale") }
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
            // Model name
            modelInfo?.let { info ->
                Text(info.name, style = ScanTypography.HeadingM, color = ScanColors.Text1)
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MetaText("Vertices", info.vertexCount.toFormattedString())
                    MetaText("Faces", info.faceCount.toFormattedString())
                    MetaText("Veličina", "${info.fileSizeKb}KB")
                }
                Spacer(Modifier.height(20.dp))
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrimaryButton(
                    "⬇ Export",
                    onClick = { navController.navigate(Screen.Export.createRoute(modelPath)) },
                    modifier = Modifier.weight(1f),
                    gradient = ScanColors.GradientPrimary
                )
                SecondaryButton(
                    "📤 Podeli",
                    onClick = { vm.shareModel() },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))

            // Quick actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip("💾 Snimi screenshot") { vm.saveScreenshot() }
                QuickActionChip("🎬 Video") { vm.recordVideo() }
                QuickActionChip("🔗 Link") { vm.copyLink() }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
    }
}

// ─── AR SceneView Composable placeholder ──────────────────────────────────────
@Composable
private fun ArSceneViewComposable(modelPath: String) {
    // In production: integrate io.github.sceneview:arsceneview
    // val engine = rememberEngine()
    // val modelLoader = rememberModelLoader(engine)
    // ARScene(
    //     modifier = Modifier.fillMaxSize(),
    //     engine = engine,
    //     modelLoader = modelLoader,
    //     ...
    // )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Animated 3D cube as placeholder
        val infiniteTransition = rememberInfiniteTransition(label = "float")
        val offsetY by infiniteTransition.animateFloat(
            0f, -12f,
            infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
            label = "float"
        )
        Box(
            modifier = Modifier
                .offset(y = offsetY.dp)
                .size(120.dp)
                .background(
                    Brush.linearGradient(
                        listOf(ScanColors.Accent.copy(0.4f), ScanColors.AccentLight.copy(0.2f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .border(1.5.dp, ScanColors.Accent.copy(0.6f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ViewInAr, null, tint = Color.White, modifier = Modifier.size(52.dp))
        }
        // Shadow
        Box(
            modifier = Modifier
                .offset(y = (60 - offsetY).dp)
                .size(80.dp, 12.dp)
                .background(
                    Brush.radialGradient(
                        listOf(ScanColors.Accent.copy(0.25f), Color.Transparent)
                    ),
                    RoundedCornerShape(50)
                )
        )
    }
}

@Composable
private fun FallbackModelViewer(modelPath: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ViewInAr, null, tint = ScanColors.AccentLight, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("3D pregled", style = ScanTypography.BodyM, color = ScanColors.Text2)
        Text("AR nije podržan na ovom uređaju", style = ScanTypography.BodyS, color = ScanColors.Text3)
    }
}

// ─── Small helpers ────────────────────────────────────────────────────────────
@Composable
private fun IconButtonGlass(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(Color.Black.copy(0.5f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun TransformToolButton(symbol: String, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(
                if (active) ScanColors.Accent.copy(0.3f) else Color.Black.copy(0.5f),
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                if (active) ScanColors.Accent else Color.White.copy(0.12f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, style = ScanTypography.BodyL, color = Color.White)
    }
}

@Composable
private fun MetaText(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = ScanTypography.Label, color = ScanColors.Text3)
        Text(value, style = ScanTypography.Mono, color = ScanColors.AccentLight)
    }
}

@Composable
private fun QuickActionChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(ScanColors.Bg2, RoundedCornerShape(20.dp))
            .border(1.dp, ScanColors.Border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, style = ScanTypography.BodyS, color = ScanColors.Text2)
    }
}

private fun Int.toFormattedString(): String {
    return if (this >= 1000) "${this / 1000}.${(this % 1000) / 100}K" else toString()
}
