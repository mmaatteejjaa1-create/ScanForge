package com.scanforge.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.scanforge.Screen
import com.scanforge.data.model.ScannedModel
import com.scanforge.ui.components.*
import com.scanforge.ui.theme.ScanColors
import com.scanforge.ui.theme.ScanTypography
import com.scanforge.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    navController: NavController,
    vm: HomeViewModel = hiltViewModel()
) {
    val models by vm.recentModels.collectAsState()
    val totalScans by vm.totalScans.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(ScanColors.Bg)) {
        // Ambient glow top-right
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = 120.dp, y = (-60).dp)
                .background(
                    Brush.radialGradient(
                        listOf(ScanColors.Accent.copy(0.12f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // Ambient glow bottom-left
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-40).dp, y = 40.dp)
                .background(
                    Brush.radialGradient(
                        listOf(ScanColors.Green.copy(0.08f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Status bar spacer
            item { Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars)) }

            // ── App bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Brush.linearGradient(ScanColors.GradientPrimary),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ViewInAr, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text("ScanForge", style = ScanTypography.HeadingM, color = ScanColors.Text1)
                            Text("3D Scanner", style = ScanTypography.BodyS, color = ScanColors.Text3)
                        }
                    }
                    IconButton(onClick = { /* Settings */ }) {
                        Icon(Icons.Default.Settings, null, tint = ScanColors.Text2, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // ── Hero heading
            item {
                Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        "Skenira sve.\nOdmah u 3D.",
                        style = ScanTypography.HeadingXL,
                        color = ScanColors.Text1
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Fotografiši sa više uglova ili upotrebi AI za brzi scan.",
                        style = ScanTypography.BodyM,
                        color = ScanColors.Text2
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }

            // ── Stats row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricTile("$totalScans", "skenova", modifier = Modifier.weight(1f))
                    MetricTile("${models.count { it.hasAr }}", "AR modela", modifier = Modifier.weight(1f), valueColor = ScanColors.Green)
                    MetricTile("${models.sumOf { it.fileSizeKb } / 1024}MB", "ukupno", modifier = Modifier.weight(1f), valueColor = ScanColors.Orange)
                }
            }

            // ── Mode cards
            item {
                SectionHeader("Novi sken", modifier = Modifier.padding(horizontal = 24.dp))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PhotoCamera,
                        title = "Photogrammetry",
                        subtitle = "30+ fotografija\nVisok kvalitet",
                        tag = "Preporučeno",
                        tagColor = ScanColors.Green,
                        gradient = ScanColors.GradientPrimary,
                        onClick = { navController.navigate(Screen.Photogrammetry.route) }
                    )
                    ModeCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AutoAwesome,
                        title = "AI Depth",
                        subtitle = "Jedan kadar\nTrenutni rezultat",
                        tag = "Brzo",
                        tagColor = ScanColors.Orange,
                        gradient = ScanColors.GradientWarm,
                        onClick = { navController.navigate(Screen.AiDepth.route) }
                    )
                }
                Spacer(Modifier.height(28.dp))
            }

            // ── Recent models header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader("Nedavni modeli")
                    if (models.isNotEmpty()) {
                        Text("Svi →", style = ScanTypography.BodyS, color = ScanColors.AccentLight,
                            modifier = Modifier.clickable { })
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Model list
            if (models.isEmpty()) {
                item { EmptyModelsPlaceholder() }
            } else {
                items(models) { model ->
                    ModelRow(
                        model = model,
                        modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp),
                        onClick = {
                            navController.navigate(Screen.ArViewer.createRoute(model.filePath))
                        }
                    )
                }
            }
        }
    }
}

// ─── Mode Card Component ──────────────────────────────────────────────────────
@Composable
private fun ModeCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    tag: String,
    tagColor: Color,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "scale")

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(ScanColors.Bg2)
            .border(
                1.dp,
                Brush.linearGradient(gradient.map { it.copy(0.3f) }),
                RoundedCornerShape(18.dp)
            )
            .clickable {
                pressed = true
                onClick()
            }
            .padding(16.dp)
    ) {
        // Icon with gradient background
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Brush.linearGradient(gradient), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, style = ScanTypography.BodyM.copy(fontWeight = FontWeight.SemiBold), color = ScanColors.Text1)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = ScanTypography.BodyS, color = ScanColors.Text2, lineHeight = 18.sp)
        Spacer(Modifier.height(12.dp))
        StatusChip(tag, tagColor)
    }
}

// ─── Model Row Component ──────────────────────────────────────────────────────
@Composable
private fun ModelRow(
    model: ScannedModel,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ScanColors.Bg2)
            .border(1.dp, ScanColors.Border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    ScanColors.Accent.copy(0.12f),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ViewInAr, null, tint = ScanColors.AccentLight, modifier = Modifier.size(24.dp))
        }

        // Info
        Column(modifier = Modifier.weight(1f)) {
            Text(model.name, style = ScanTypography.BodyM.copy(fontWeight = FontWeight.SemiBold), color = ScanColors.Text1)
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (model.mode == "photogrammetry") "Photogrammetry" else "AI Depth",
                    style = ScanTypography.BodyS,
                    color = ScanColors.Text3
                )
                Text("·", style = ScanTypography.BodyS, color = ScanColors.Text3)
                Text(
                    SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(model.createdAt)),
                    style = ScanTypography.BodyS,
                    color = ScanColors.Text3
                )
            }
        }

        // Size + tag
        Column(horizontalAlignment = Alignment.End) {
            StatusChip(
                if (model.mode == "photogrammetry") "GLB" else "AI",
                if (model.mode == "photogrammetry") ScanColors.Green else ScanColors.Orange
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${model.fileSizeKb}KB",
                style = ScanTypography.BodyS,
                color = ScanColors.Text3,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun EmptyModelsPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ViewInAr, null, tint = ScanColors.Text3, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text("Još nema modela", style = ScanTypography.BodyM, color = ScanColors.Text3)
        Text("Napravi prvi sken gore", style = ScanTypography.BodyS, color = ScanColors.Text3)
    }
}
