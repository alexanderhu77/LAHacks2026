package com.lahacks2026.pretriage.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    NavHost(
        navController = navController,
        startDestination = Screen.Intake.route
    ) {
        composable(Screen.Intake.route) {
            IntakeScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToResult = { navController.navigate(Screen.Result.route) }
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
                onNavigateBack = { navController.popBackStack(Screen.Intake.route, inclusive = false) }
            )
        }
    }
}
