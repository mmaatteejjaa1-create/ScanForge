package com.scanforge.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.scanforge.ui.components.*
import com.scanforge.ui.theme.ScanColors
import com.scanforge.ui.theme.ScanTypography
import com.scanforge.viewmodel.ExportViewModel

data class ExportFormat(val ext: String, val desc: String, val compatible: List<String>)

@Composable
fun ExportScreen(
    navController: NavController,
    modelPath: String,
    vm: ExportViewModel = hiltViewModel()
) {
    val modelInfo by vm.modelInfo.collectAsState()
    val selectedFormat by vm.selectedFormat.collectAsState()
    val texturesEnabled by vm.texturesEnabled.collectAsState()
    val compressionEnabled by vm.compressionEnabled.collectAsState()
    val textureResolution by vm.textureResolution.collectAsState()
    val isExporting by vm.isExporting.collectAsState()
    val exportSuccess by vm.exportSuccess.collectAsState()

    LaunchedEffect(modelPath) { vm.loadModel(modelPath) }

    val formats = listOf(
        ExportFormat(".glb", "Binarni glTF — kompaktan, sve u jednom fajlu", listOf("Blender", "Unity", "Unreal", "Web")),
        ExportFormat(".gltf", "glTF sa eksternim resursima", listOf("Blender", "Three.js", "Web")),
        ExportFormat(".obj", "Wavefront OBJ — univerzalno podržan", listOf("Blender", "Maya", "3ds Max", "Cinema4D")),
        ExportFormat(".stl", "Stereolithography — idealno za 3D print", listOf("PrusaSlicer", "Cura", "Simplify3D")),
        ExportFormat(".fbx", "Autodesk FBX — game engine standard", listOf("Unity", "Unreal", "Maya")),
        ExportFormat(".ply", "Polygon File Format — istraživanje, point cloud", listOf("MeshLab", "CloudCompare"))
    )

    Box(modifier = Modifier.fillMaxSize().background(ScanColors.Bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            // ── Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ScanColors.Bg2, RoundedCornerShape(12.dp))
                        .border(1.dp, ScanColors.Border, RoundedCornerShape(12.dp))
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ArrowBack, null, tint = ScanColors.Text2, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Export modela", style = ScanTypography.HeadingM, color = ScanColors.Text1)
                    modelInfo?.let {
                        Text(it.name, style = ScanTypography.BodyS, color = ScanColors.Text3)
                    }
                }
            }

            // ── Preview card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(160.dp)
                    .background(ScanColors.Bg2, RoundedCornerShape(16.dp))
                    .border(1.dp, ScanColors.Border, RoundedCornerShape(16.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(ScanColors.Accent.copy(0.1f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.ViewInAr, null, tint = ScanColors.AccentLight, modifier = Modifier.size(56.dp))
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, ScanColors.Bg2)),
                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    modelInfo?.let {
                        Text(
                            "${it.vertexCount.toFormattedString()} vertices · ${it.faceCount.toFormattedString()} faces",
                            style = ScanTypography.BodyS,
                            color = ScanColors.Text2,
                            fontFamily = FontFamily.Monospace
                        )
                        StatusChip("${it.fileSizeKb}KB", ScanColors.Accent)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Format selector
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                SectionHeader("Format fajla")
                Spacer(Modifier.height(12.dp))

                formats.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { fmt ->
                            FormatChip(
                                format = fmt,
                                selected = selectedFormat == fmt.ext,
                                modifier = Modifier.weight(1f),
                                onClick = { vm.selectFormat(fmt.ext) }
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }

                // Selected format compatibility
                formats.find { it.ext == selectedFormat }?.let { fmt ->
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ScanColors.Accent.copy(0.07f), RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null, tint = ScanColors.AccentLight, modifier = Modifier.size(16.dp))
                        Text(
                            "Kompatibilan sa: ${fmt.compatible.joinToString(", ")}",
                            style = ScanTypography.BodyS,
                            color = ScanColors.Text2
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Settings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(ScanColors.Bg2, RoundedCornerShape(16.dp))
                    .border(1.dp, ScanColors.Border, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                SectionHeader("Opcije exporta")
                Spacer(Modifier.height(16.dp))

                SettingToggle("Teksture uključene", texturesEnabled) { vm.setTexturesEnabled(it) }
                Divider(color = ScanColors.Border, modifier = Modifier.padding(vertical = 12.dp))

                SettingToggle("Mesh kompresija (Draco)", compressionEnabled) { vm.setCompressionEnabled(it) }
                Divider(color = ScanColors.Border, modifier = Modifier.padding(vertical = 12.dp))

                SettingSelect(
                    "Rezolucija teksture",
                    textureResolution,
                    listOf("512×512", "1024×1024", "2048×2048", "4096×4096")
                ) { vm.setTextureResolution(it) }
                Divider(color = ScanColors.Border, modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Koordinatni sistem", style = ScanTypography.BodyM, color = ScanColors.Text1)
                    Text("Y-up (WebGL standard)", style = ScanTypography.BodyS, color = ScanColors.AccentLight, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Export button
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                AnimatedVisibility(visible = exportSuccess) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ScanColors.Green.copy(0.1f), RoundedCornerShape(12.dp))
                            .border(1.dp, ScanColors.Green.copy(0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = ScanColors.Green, modifier = Modifier.size(20.dp))
                        Text("Model uspješno sačuvan na uređaj!", style = ScanTypography.BodyM, color = ScanColors.Green)
                    }
                    Spacer(Modifier.height(12.dp))
                }

                PrimaryButton(
                    if (isExporting) "Eksportuje se..." else "⬇ Sačuvaj na uređaj",
                    onClick = { vm.exportModel(modelPath) },
                    isLoading = isExporting,
                    gradient = ScanColors.GradientSuccess
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SecondaryButton("📤 Podeli", onClick = { vm.shareModel(modelPath) }, modifier = Modifier.weight(1f))
                    SecondaryButton("📋 Kopiraj putanju", onClick = { vm.copyPath(modelPath) }, modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FormatChip(
    format: ExportFormat,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (selected) ScanColors.Accent.copy(0.15f) else ScanColors.Bg2
            )
            .border(
                1.dp,
                if (selected) ScanColors.Accent else ScanColors.Border,
                RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            format.ext,
            style = ScanTypography.Mono,
            color = if (selected) ScanColors.AccentLight else ScanColors.Text2
        )
    }
}

@Composable
private fun SettingToggle(label: String, enabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = ScanTypography.BodyM, color = ScanColors.Text1)
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ScanColors.Accent,
                uncheckedThumbColor = ScanColors.Text3,
                uncheckedTrackColor = ScanColors.Bg3
            )
        )
    }
}

@Composable
private fun SettingSelect(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = ScanTypography.BodyM, color = ScanColors.Text1)
        Box {
            Row(
                modifier = Modifier
                    .background(ScanColors.Bg3, RoundedCornerShape(8.dp))
                    .border(1.dp, ScanColors.Border, RoundedCornerShape(8.dp))
                    .clickable { expanded = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(value, style = ScanTypography.BodyS, color = ScanColors.AccentLight, fontFamily = FontFamily.Monospace)
                Icon(Icons.Default.ExpandMore, null, tint = ScanColors.Text3, modifier = Modifier.size(14.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(ScanColors.Bg2)
            ) {
                options.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt, style = ScanTypography.BodyS, color = ScanColors.Text1, fontFamily = FontFamily.Monospace) },
                        onClick = { onSelect(opt); expanded = false }
                    )
                }
            }
        }
    }
}

private fun Int.toFormattedString(): String =
    if (this >= 1000) "${this / 1000}.${(this % 1000) / 100}K" else toString()
