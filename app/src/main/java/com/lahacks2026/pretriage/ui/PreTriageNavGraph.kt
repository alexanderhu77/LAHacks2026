package com.lahacks2026.pretriage.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lahacks2026.pretriage.data.SeverityLevel
import com.lahacks2026.pretriage.privacy.AnonymizerService
import com.lahacks2026.pretriage.privacy.InMemoryPhiTokenMap
import com.lahacks2026.pretriage.privacy.MockTelehealthClient
import com.lahacks2026.pretriage.privacy.RegexAnonymizer
import com.lahacks2026.pretriage.ui.camera.CameraOfferScreen
import com.lahacks2026.pretriage.ui.camera.CameraScreen
import com.lahacks2026.pretriage.ui.deid.DeidUploadScreen
import com.lahacks2026.pretriage.ui.intake.IntakeScreen
import com.lahacks2026.pretriage.ui.permissions.PermissionsScreen
import com.lahacks2026.pretriage.ui.result.ResultScreen
import com.lahacks2026.pretriage.ui.splash.SplashScreen
import com.lahacks2026.pretriage.ui.triaging.TriagingScreen

private object Routes {
    const val Splash = "splash"
    const val Permissions = "permissions"
    const val Intake = "intake"
    const val CameraOffer = "cameraOffer"
    const val CameraCapture = "cameraCapture"
    const val Triaging = "triaging"
    const val Result = "result"
    const val Deid = "deid"
}

@Composable
fun PreTriageNavGraph(
    navController: NavHostController = rememberNavController(),
) {
    val viewModel: AppViewModel = viewModel()
    val state by viewModel.state.collectAsState()

    val anonymizer: AnonymizerService = remember { RegexAnonymizer() }
    val tokenMap = remember { InMemoryPhiTokenMap() }
    val telehealth = remember { MockTelehealthClient() }

    NavHost(navController = navController, startDestination = Routes.Splash) {

        composable(Routes.Splash) {
            SplashScreen(
                warmup = state.warmup,
                onStart = { viewModel.runWarmup() },
                onDone = {
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
                hasImage = state.image != null,
                onTranscriptChange = viewModel::setTranscript,
                onContinue = {
                    if (mentionsImageySymptom(state.intake.transcript) && state.image == null) {
                        navController.navigate(Routes.CameraOffer)
                    } else {
                        viewModel.runTriage()
                        navController.navigate(Routes.Triaging)
                    }
                },
                onCamera = {
                    navController.navigate(Routes.CameraCapture)
                },
                onEmergencyShortCircuit = {
                    viewModel.runTriage()
                    navController.navigate(Routes.Result) {
                        popUpTo(Routes.Intake) { inclusive = false }
                    }
                },
            )
        }

        composable(Routes.CameraOffer) {
            CameraOfferScreen(
                onTakePhoto = { navController.navigate(Routes.CameraCapture) },
                onSkip = {
                    viewModel.runTriage()
                    navController.navigate(Routes.Triaging) {
                        popUpTo(Routes.CameraOffer) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.CameraCapture) {
            CameraScreen(
                onPhotoCaptured = { bm ->
                    viewModel.setImage(bm)
                    viewModel.runTriage()
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
                decisionAvailable = state.decision != null,
                onDone = {
                    navController.navigate(Routes.Result) {
                        popUpTo(Routes.Intake) { inclusive = false }
                    }
                },
            )
        }

        composable(Routes.Result) {
            val decision = state.decision
            if (decision != null) {
                ResultScreen(
                    decision = decision,
                    plan = state.plan,
                    isShortCircuit = decision.severity == SeverityLevel.EMERGENCY &&
                        decision.redFlags.isNotEmpty() &&
                        decision.confidence == 1.0,
                    onRestart = {
                        viewModel.resetSession()
                        navController.navigate(Routes.Splash) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSendLabs = { navController.navigate(Routes.Deid) },
                )
            }
        }

        composable(Routes.Deid) {
            DeidUploadScreen(
                phase = state.deid.phase,
                onPhaseChange = viewModel::setDeidPhase,
                anonymizer = anonymizer,
                tokenMap = tokenMap,
                telehealth = telehealth,
                onBack = { navController.popBackStack() },
                onDone = {
                    viewModel.resetSession()
                    tokenMap.clear()
                    navController.navigate(Routes.Splash) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}

private val IMAGEY_KEYWORDS = listOf(
    "rash", "mole", "bump", "spot", "skin", "wound", "cut", "scrape",
    "eye", "swelling", "swollen", "bruise", "redness", "lesion", "burn",
    "bite", "sting", "mark", "scab", "blister", "ulcer", "patch", "lump",
    "discoloration", "injury", "blood", "bleed", "broken",
)

private fun mentionsImageySymptom(transcript: String): Boolean {
    val t = transcript.lowercase()
    return IMAGEY_KEYWORDS.any { it in t }
}
