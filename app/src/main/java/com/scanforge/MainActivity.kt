package com.scanforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.scanforge.ui.screens.*
import com.scanforge.ui.theme.ScanColors
import com.scanforge.ui.theme.ScanForgeTheme
import dagger.hilt.android.AndroidEntryPoint

sealed class Screen(val route: String) {
    object Home             : Screen("home")
    object Photogrammetry   : Screen("photogrammetry")
    object AiDepth          : Screen("ai_depth")
    object Processing       : Screen("processing/{mode}") {
        fun createRoute(mode: String) = "processing/$mode"
    }
    object ArViewer         : Screen("ar_viewer/{modelPath}") {
        fun createRoute(modelPath: String) = "ar_viewer/${modelPath.replace("/", "|")}"
    }
    object Export           : Screen("export/{modelPath}") {
        fun createRoute(modelPath: String) = "export/${modelPath.replace("/", "|")}"
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScanForgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ScanColors.Bg
                ) {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(navController)
                        }
                        composable(Screen.Photogrammetry.route) {
                            PhotogrammetryScreen(navController)
                        }
                        composable(Screen.AiDepth.route) {
                            AiDepthScreen(navController)
                        }
                        composable(Screen.Processing.route) { back ->
                            val mode = back.arguments?.getString("mode") ?: "photogrammetry"
                            ProcessingScreen(navController, mode)
                        }
                        composable(Screen.ArViewer.route) { back ->
                            val path = back.arguments?.getString("modelPath")
                                ?.replace("|", "/") ?: ""
                            ArViewerScreen(navController, path)
                        }
                        composable(Screen.Export.route) { back ->
                            val path = back.arguments?.getString("modelPath")
                                ?.replace("|", "/") ?: ""
                            ExportScreen(navController, path)
                        }
                    }
                }
            }
        }
    }
}
