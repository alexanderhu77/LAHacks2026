package com.lahacks2026.pretriage.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lahacks2026.pretriage.AppViewModel
import com.lahacks2026.pretriage.DeidPhase
import com.lahacks2026.pretriage.data.IntentHint
import com.lahacks2026.pretriage.ui.cameraoffer.CameraOfferScreen
import com.lahacks2026.pretriage.ui.camera.CameraScreen
import com.lahacks2026.pretriage.ui.deid.DeidUploadScreen
import com.lahacks2026.pretriage.ui.diagnostics.SmokeTestScreen
import com.lahacks2026.pretriage.ui.intake.IntakeScreen
import com.lahacks2026.pretriage.ui.permissions.PermissionsScreen
import com.lahacks2026.pretriage.ui.result.ResultScreen
import com.lahacks2026.pretriage.ui.splash.SplashScreen
import com.lahacks2026.pretriage.ui.triaging.TriagingScreen

object Routes {
    const val Splash = "splash"
    const val Permissions = "permissions"
    const val Intake = "intake"
    const val CameraOffer = "cameraOffer"
    const val Camera = "camera"
    const val Triaging = "triaging"
    const val Result = "result"
    const val Deid = "deid"
    const val Diagnostics = "diagnostics"
}

@Composable
fun PreTriageNavGraph(
    vm: AppViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.warmUp() }

    NavHost(navController = navController, startDestination = Routes.Splash) {
        composable(Routes.Splash) {
            SplashScreen(
                warmup = state.warmup,
                onWarmupComplete = {
                    navController.navigate(Routes.Permissions) {
                        popUpTo(Routes.Splash) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Permissions) {
            PermissionsScreen(
                onContinue = {
                    navController.navigate(Routes.Intake) {
                        popUpTo(Routes.Permissions) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Intake) {
            IntakeScreen(
                transcript = state.intake.transcript,
                recording = state.intake.recording,
                onTranscriptChange = vm::setTranscript,
                onRecordingChange = vm::setRecording,
                onContinue = {
                    if (state.intake.emergencyShortCircuit) {
                        navController.navigate(Routes.Triaging)
                        return@IntakeScreen
                    }
                    if (mayBeImagey(state.intake.transcript)) {
                        navController.navigate(Routes.CameraOffer)
                    } else {
                        navController.navigate(Routes.Triaging)
                    }
                },
                onOpenDiagnostics = { navController.navigate(Routes.Diagnostics) },
            )
        }
        composable(Routes.CameraOffer) {
            CameraOfferScreen(
                onSkip = { navController.navigate(Routes.Triaging) },
                onCapture = { navController.navigate(Routes.Camera) },
            )
        }
        composable(Routes.Camera) {
            CameraScreen(
                onPhotoCaptured = { bitmap ->
                    vm.setImage(bitmap)
                    navController.navigate(Routes.Triaging) {
                        popUpTo(Routes.CameraOffer) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(Routes.Triaging) {
            TriagingScreen(
                withImage = state.image != null,
                inFlight = state.triageInFlight,
                onTriageRequested = {
                    vm.runTriage {
                        navController.navigate(Routes.Result) {
                            popUpTo(Routes.Triaging) { inclusive = true }
                        }
                    }
                },
            )
        }
        composable(Routes.Result) {
            val decision = state.decision
            if (decision == null) {
                LaunchedEffect(Unit) { navController.popBackStack(Routes.Intake, false) }
                return@composable
            }
            ResultScreen(
                decision = decision,
                emergencyShortCircuit = state.intake.emergencyShortCircuit,
                onRestart = {
                    vm.resetSession()
                    navController.navigate(Routes.Intake) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onAction = { hint -> dispatchIntent(context, hint, decision.recommendedAction.provider) },
                onUploadDocs = {
                    vm.setDeidPhase(DeidPhase.Preview)
                    navController.navigate(Routes.Deid)
                },
            )
        }
        composable(Routes.Deid) {
            DeidUploadScreen(
                phase = state.deid.phase,
                onBack = { navController.popBackStack() },
                onPhaseChange = vm::setDeidPhase,
                onDone = {
                    vm.resetSession()
                    navController.navigate(Routes.Intake) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Diagnostics) {
            SmokeTestScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}

private fun mayBeImagey(transcript: String): Boolean {
    val lower = transcript.lowercase()
    return listOf("rash", "mole", "skin", "wound", "cut", "eye", "bruise", "swelling", "lesion", "bump")
        .any { it in lower }
}

private fun dispatchIntent(context: android.content.Context, hint: IntentHint, provider: String) {
    val intent = when (hint) {
        IntentHint.DIAL_911 -> Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
        IntentHint.OPEN_TELEHEALTH_DEEP_LINK -> Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/telehealth"))
        IntentHint.MAPS_QUERY_URGENT_CARE -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=urgent+care"))
        IntentHint.SHOW_SELF_CARE_TEXT -> null
    } ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
