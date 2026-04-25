package com.lahacks2026.pretriage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.lahacks2026.pretriage.data.*
import com.lahacks2026.pretriage.ui.camera.CameraScreen
import com.lahacks2026.pretriage.ui.diagnostics.SmokeTestScreen
import com.lahacks2026.pretriage.ui.intake.IntakeScreen
import com.lahacks2026.pretriage.ui.result.ResultScreen

sealed class Screen(val route: String) {
    object Intake : Screen("intake")
    object Camera : Screen("camera")
    object Result : Screen("result")
    object Diagnostics : Screen("diagnostics")
}

@Composable
fun PreTriageNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    // Shared state for the demo
    var currentScenario by remember { mutableStateOf<DemoScenario?>(null) }
    
    // Load Alex's mock plan for the UI/UX demo
    val mockPlan = remember {
        InsurancePlanLoader.load(context, "ppo")
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Intake.route
    ) {
        composable(Screen.Intake.route) {
            IntakeScreen(
                onNavigateToCamera = { scenario ->
                    currentScenario = scenario
                    navController.navigate(Screen.Camera.route)
                },
                onNavigateToResult = { scenario ->
                    currentScenario = scenario
                    navController.navigate(Screen.Result.route)
                },
                onNavigateToDiagnostics = { navController.navigate(Screen.Diagnostics.route) }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(
                onPhotoCaptured = { navController.navigate(Screen.Result.route) },
                onCancel = { navController.popBackStack() }
            )
        }
        composable(Screen.Result.route) {
            ResultScreen(
                plan = mockPlan,
                scenario = currentScenario,
                onNavigateBack = { 
                    currentScenario = null
                    navController.popBackStack(Screen.Intake.route, inclusive = false) 
                }
            )
        }
        composable(Screen.Diagnostics.route) {
            SmokeTestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
