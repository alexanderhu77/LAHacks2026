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
import com.lahacks2026.pretriage.ui.chat.ChatScreen
import com.lahacks2026.pretriage.ui.deid.DeidUploadScreen
import com.lahacks2026.pretriage.ui.intake.IntakeScreen
import com.lahacks2026.pretriage.ui.network.PickNetworkScreen
import com.lahacks2026.pretriage.ui.permissions.PermissionsScreen
import com.lahacks2026.pretriage.ui.provider.FindProviderScreen
import com.lahacks2026.pretriage.ui.result.ResultScreen
import com.lahacks2026.pretriage.ui.splash.SplashScreen
import com.lahacks2026.pretriage.ui.triaging.TriagingScreen

private object Routes {
    const val Splash = "splash"
    const val Permissions = "permissions"
    const val PickNetwork = "pickNetwork"
    const val Intake = "intake"
    const val Chat = "chat"
    const val CameraOffer = "cameraOffer"
    const val CameraCapture = "cameraCapture"
    const val Triaging = "triaging"
    const val Result = "result"
    const val FindProvider = "findProvider"
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
                    // If the network was already picked in a prior session (state survived),
                    // skip past the picker straight to intake.
                    val target = if (state.selectedNetwork != null) Routes.Intake else Routes.PickNetwork
                    navController.navigate(target) {
                        popUpTo(Routes.Permissions) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.PickNetwork) {
            PickNetworkScreen(
                selected = state.selectedNetwork,
                onPick = viewModel::setNetwork,
                onContinue = {
                    navController.navigate(Routes.Intake) {
                        popUpTo(Routes.PickNetwork) { inclusive = true }
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
                    navController.navigate(Routes.Chat)
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

        composable(Routes.Chat) {
            ChatScreen(
                state = state.chat,
                image = state.image,
                onComposerChange = viewModel::setComposer,
                onChatRecordingChange = viewModel::setChatRecording,
                onSend = viewModel::sendUserMessage,
                onAdvanceNoraTurn = viewModel::advanceNoraTurn,
                onAppendPhotoBubbleIfNeeded = viewModel::appendPhotoBubbleIfNeeded,
                onAppendSkippedPhoto = viewModel::appendSkippedPhoto,
                onStartChatIfNeeded = viewModel::startChatIfNeeded,
                onRequestPhoto = { navController.navigate(Routes.CameraOffer) },
                onReadyToTriage = {
                    viewModel.runTriage()
                    navController.navigate(Routes.Triaging)
                },
                onEmergencyShortCircuit = {
                    viewModel.runTriage()
                    navController.navigate(Routes.Result)
                },
                onRestart = {
                    viewModel.resetSession()
                    navController.navigate(Routes.Splash) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CameraOffer) {
            CameraOfferScreen(
                onTakePhoto = { navController.navigate(Routes.CameraCapture) },
                onSkip = {
                    viewModel.appendSkippedPhoto()
                    navController.popBackStack(Routes.Chat, false)
                },
            )
        }

        composable(Routes.CameraCapture) {
            CameraScreen(
                onPhotoCaptured = { bm ->
                    viewModel.setImage(bm)
                    navController.popBackStack(Routes.Chat, false)
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
                    diagnosticSummary = state.diagnosticSummary,
                    diagnosticSummaryLoading = state.diagnosticSummaryLoading,
                    onRestart = {
                        viewModel.resetSession()
                        navController.navigate(Routes.Splash) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSendLabs = { navController.navigate(Routes.Deid) },
                    onFindProvider = { navController.navigate(Routes.FindProvider) },
                )
            }
        }

        composable(Routes.FindProvider) {
            FindProviderScreen(
                network = state.selectedNetwork,
                onBack = { navController.popBackStack() },
            )
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
