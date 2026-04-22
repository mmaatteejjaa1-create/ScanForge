package com.scanforge.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.scanforge.Screen
import com.scanforge.ui.components.PrimaryButton
import com.scanforge.ui.components.ScanProgressRing
import com.scanforge.ui.theme.ScanColors
import com.scanforge.ui.theme.ScanTypography
import com.scanforge.viewmodel.ProcessingViewModel

@Composable
fun ProcessingScreen(
    navController: NavController,
    mode: String,
    vm: ProcessingViewModel = hiltViewModel()
) {
    val progress by vm.progress.collectAsState()
    val currentStep by vm.currentStep.collectAsState()
    val isComplete by vm.isComplete.collectAsState()
    val outputPath by vm.outputModelPath.collectAsState()
    val steps by vm.processingSteps.collectAsState()

    LaunchedEffect(mode) { vm.startProcessing(mode) }

    LaunchedEffect(isComplete, outputPath) {
        if (isComplete && outputPath.isNotEmpty()) {
            kotlinx.coroutines.delay(800)
            navController.navigate(Screen.ArViewer.createRoute(outputPath)) {
                popUpTo(Screen.Home.route)
            }
        }
    }

    // Rotating particles background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val rotation by infiniteTransition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(12000, easing = LinearEasing)),
        label = "rot"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScanColors.Bg),
        contentAlignment = Alignment.Center
    ) {
        // Ambient blobs
        Box(
            modifier = Modifier
                .size(400.dp)
                .rotate(rotation)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            ScanColors.Accent.copy(0.06f),
                            Color.Transparent,
                            ScanColors.Green.copy(0.04f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))

            // Main progress ring
            ScanProgressRing(
                progress = progress,
                size = 160.dp,
                strokeWidth = 8.dp
            )
            Spacer(Modifier.height(32.dp))

            // Step title
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInVertically { it } + fadeIn() togetherWith
                    slideOutVertically { -it } + fadeOut()
                },
                label = "step"
            ) { step ->
                Text(
                    step,
                    style = ScanTypography.HeadingM,
                    color = ScanColors.Text1,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (mode == "photogrammetry") "Photogrammetry procesiranje" else "AI dubinska analiza",
                style = ScanTypography.BodyM,
                color = ScanColors.Text3,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            // Steps list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ScanColors.Bg2, RoundedCornerShape(16.dp))
                    .border(1.dp, ScanColors.Border, RoundedCornerShape(16.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                steps.forEachIndexed { index, step ->
                    ProcessingStepRow(
                        step = step,
                        state = when {
                            index < steps.indexOfFirst { !it.isDone } -> StepState.Done
                            index == steps.indexOfFirst { !it.isDone } -> StepState.Active
                            else -> StepState.Pending
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Complete state
            AnimatedVisibility(visible = isComplete) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PrimaryButton(
                        "🥽 Otvori u AR",
                        onClick = {
                            navController.navigate(Screen.ArViewer.createRoute(outputPath)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        gradient = ScanColors.GradientSuccess
                    )
                }
            }
        }
    }
}

enum class StepState { Done, Active, Pending }

data class ProcessingStep(val label: String, val isDone: Boolean)

@Composable
private fun ProcessingStepRow(step: ProcessingStep, state: StepState) {
    val infiniteTransition = rememberInfiniteTransition(label = "active")
    val pulseAlpha by infiniteTransition.animateFloat(
        1f, 0.3f,
        infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status icon
        Box(
            modifier = Modifier.size(28.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                StepState.Done -> Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = ScanColors.Green,
                    modifier = Modifier.size(22.dp)
                )
                StepState.Active -> CircularProgressIndicator(
                    color = ScanColors.Accent,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                StepState.Pending -> Box(
                    modifier = Modifier
                        .size(20.dp)
                        .border(1.5.dp, ScanColors.Border, CircleShape)
                )
            }
        }

        // Label
        Text(
            step.label,
            style = ScanTypography.BodyM,
            color = when (state) {
                StepState.Done -> ScanColors.Text2
                StepState.Active -> ScanColors.Text1
                StepState.Pending -> ScanColors.Text3
            },
            modifier = if (state == StepState.Active) Modifier.alpha(pulseAlpha) else Modifier
        )
    }
}
