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
import com.google.gson.Gson
import com.lahacks2026.pretriage.data.*
import com.lahacks2026.pretriage.ui.camera.CameraScreen
import com.lahacks2026.pretriage.ui.intake.IntakeScreen
import com.lahacks2026.pretriage.ui.result.ResultScreen

sealed class Screen(val route: String) {
    object Intake : Screen("intake")
    object Camera : Screen("camera")
    object Result : Screen("result")
}

@Composable
fun PreTriageNavGraph(
    navController: NavHostController = rememberNavController()
) {
    // Shared state for the demo
    var currentScenario by remember { mutableStateOf<DemoScenario?>(null) }
    
    // Mock the current plan
    val mockPlan = remember {
        InsurancePlan(
            plan_name = "BlueShield PPO Premium",
            telehealth = TelehealthInfo("Teladoc Health", 0, "https://teladoc.com"),
            urgent_care = ProviderInfo("City Health Urgent Care", 20, "1.2 miles"),
            emergency = EmergencyInfo(250, "Waived if admitted")
        )
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
                }
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
    }
}
