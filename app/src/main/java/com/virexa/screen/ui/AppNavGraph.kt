package com.virexa.screen.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.virexa.screen.data.QualityOption
import com.virexa.screen.ui.components.BottomTabBar
import com.virexa.screen.ui.screens.*

sealed class Dest(val route: String) {
    data object Splash : Dest("splash")
    data object Onboarding : Dest("onboarding")
    data object Home : Dest("home")
    data object Library : Dest("library")
    data object Settings : Dest("settings")
    data object AdvancedSettings : Dest("advanced_settings")
    data object Detail : Dest("detail/{path}") {
        fun create(path: String) = "detail/${android.net.Uri.encode(path)}"
    }
}

@Composable
fun AppNavGraph(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val prefs by viewModel.preferences.collectAsState()
    val recordingState by viewModel.recordingUiState.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in setOf(Dest.Home.route, Dest.Library.route, Dest.Settings.route)

    VirexaTheme(themeMode = prefs.themeMode) {
        val context = LocalContext.current
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = Dest.Splash.route,
                modifier = Modifier.fillMaxSize().padding(bottom = if (showBottomBar) 108.dp else 0.dp),
            ) {
                // ── Splash
                composable(Dest.Splash.route) {
                    SplashScreen {
                        navController.navigate(if (prefs.onboardingCompleted) Dest.Home.route else Dest.Onboarding.route) {
                            popUpTo(Dest.Splash.route) { inclusive = true }
                        }
                    }
                }

                // ── Onboarding
                composable(Dest.Onboarding.route) {
                    OnboardingScreen(
                        preferences = prefs,
                        onFinish = {
                            viewModel.completeOnboarding()
                            navController.navigate(Dest.Home.route) { popUpTo(Dest.Onboarding.route) { inclusive = true } }
                        },
                        onUpdateName = viewModel::updateProfileName,
                        onUpdateLanguage = viewModel::updateLanguage,
                        onUpdateTheme = viewModel::updateThemeMode,
                        onUpdateBubble = viewModel::updateFloatingBubbleEnabled,
                        onUpdateAudio = viewModel::updateDefaultAudioMode,
                        onUpdateQuality = viewModel::updateDefaultQuality,
                    )
                }

                // ── Home
                composable(Dest.Home.route) {
                    var waitingForMicPermission by remember { mutableStateOf(false) }
                    var pendingCaptureIntent by remember { mutableStateOf<Intent?>(null) }

                    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                            viewModel.startRecording(
                                permissionResultCode = result.resultCode,
                                permissionData = result.data!!,
                                quality = QualityOption.fromId(prefs.defaultQualityId),
                                audioMode = prefs.defaultAudioMode,
                            )
                        }
                        pendingCaptureIntent = null
                        waitingForMicPermission = false
                    }
                    val microphonePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                        if (granted && waitingForMicPermission) pendingCaptureIntent?.let(captureLauncher::launch)
                        waitingForMicPermission = false
                    }
                    val overlayPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        if (Settings.canDrawOverlays(context) && prefs.floatingBubbleEnabled) viewModel.startBubbleService()
                    }

                    HomeScreen(
                        preferences = prefs,
                        recordingState = recordingState,
                        onStartRecording = {
                            val projectionManager = context.getSystemService(MediaProjectionManager::class.java)
                            val captureIntent = projectionManager?.createScreenCaptureIntent()
                            if (captureIntent != null) {
                                if (prefs.defaultAudioMode.usesMicrophone && ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                    waitingForMicPermission = true
                                    pendingCaptureIntent = captureIntent
                                    microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                } else {
                                    captureLauncher.launch(captureIntent)
                                }
                            }
                        },
                        onStopRecording = viewModel::stopRecording,
                        onPauseRecording = viewModel::pauseRecording,
                        onResumeRecording = viewModel::resumeRecording,
                        onOpenLibrary = { navController.navigate(Dest.Library.route) },
                        onOpenSettings = { navController.navigate(Dest.Settings.route) },
                        onEnableBubble = {
                            if (Settings.canDrawOverlays(context)) viewModel.startBubbleService()
                            else overlayPermissionLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}")))
                        },
                        onRefresh = viewModel::refreshRecordings,
                    )
                }

                // ── Library
                composable(Dest.Library.route) {
                    LibraryScreen(
                        recordings = recordings,
                        onBack = { navController.popBackStack() },
                        onOpen = { navController.navigate(Dest.Detail.create(it.filePath)) },
                        onDelete = viewModel::deleteRecording,
                        onRefresh = viewModel::refreshRecordings,
                    )
                }

                // ── Settings
                composable(Dest.Settings.route) {
                    SettingsScreen(
                        preferences = prefs,
                        onBack = { navController.popBackStack() },
                        onUpdateName = viewModel::updateProfileName,
                        onUpdateLanguage = viewModel::updateLanguage,
                        onUpdateTheme = viewModel::updateThemeMode,
                        onUpdateBubble = viewModel::updateFloatingBubbleEnabled,
                        onUpdateQuickControls = viewModel::updateShowQuickControls,
                        onUpdateAudio = viewModel::updateDefaultAudioMode,
                        onUpdateQuality = viewModel::updateDefaultQuality,
                        onUpdateFolder = viewModel::updateOutputFolder,
                        onStartBubble = { if (Settings.canDrawOverlays(context)) viewModel.startBubbleService() },
                        onOpenAdvanced = { navController.navigate(Dest.AdvancedSettings.route) },
                    )
                }

                // ── Advanced Settings
                composable(Dest.AdvancedSettings.route) {
                    AdvancedSettingsScreen(
                        preferences = prefs,
                        onBack = { navController.popBackStack() },
                        onUpdateEncoder = viewModel::updateVideoEncoder,
                        onUpdateBitrateMode = viewModel::updateBitrateMode,
                        onUpdateCustomBitrate = viewModel::updateCustomBitrateMbps,
                        onUpdateFrameRate = viewModel::updateFrameRate,
                        onUpdateShowTimerOnBubble = viewModel::updateShowTimerOnBubble,
                        onUpdateAutoPauseOnCall = viewModel::updateAutoPauseOnCall,
                        onUpdateKeepScreenOn = viewModel::updateKeepScreenOn,
                        onUpdateShowTouchIndicator = viewModel::updateShowTouchIndicator,
                    )
                }

                // ── Recording Detail
                composable(
                    route = Dest.Detail.route,
                    arguments = listOf(navArgument("path") { type = NavType.StringType }),
                ) { backStack ->
                    val path = backStack.arguments?.getString("path").orEmpty()
                    val recording = recordings.firstOrNull { it.filePath == android.net.Uri.decode(path) }
                    if (recording != null) {
                        RecordingDetailScreen(
                            recording = recording,
                            onBack = { navController.popBackStack() },
                            onDelete = { viewModel.deleteRecording(recording); navController.popBackStack() },
                            onRename = { newName -> viewModel.renameRecording(recording, newName) },
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Grabación no encontrada") }
                    }
                }
            }

            AnimatedVisibility(
                visible = showBottomBar,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                BottomTabBar(
                    currentRoute = when (currentRoute) { Dest.Home.route -> "home"; Dest.Library.route -> "library"; Dest.Settings.route -> "settings"; else -> null },
                    onHome = { navController.navigate(Dest.Home.route) { popUpTo(Dest.Home.route) { inclusive = true }; launchSingleTop = true } },
                    onLibrary = { navController.navigate(Dest.Library.route) { launchSingleTop = true } },
                    onSettings = { navController.navigate(Dest.Settings.route) { launchSingleTop = true } },
                )
            }
        }
    }
}
