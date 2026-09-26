package com.fidfanstudios.fidfancullr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fidfanstudios.fidfancullr.ui.screens.CullingScreen
import com.fidfanstudios.fidfancullr.ui.screens.OnboardingScreen
import com.fidfanstudios.fidfancullr.ui.screens.SettingsScreen
import com.fidfanstudios.fidfancullr.ui.theme.FidFanCullrTheme
import com.fidfanstudios.fidfancullr.viewmodel.CullingViewModel
import com.fidfanstudios.fidfancullr.viewmodel.SettingsViewModel

private object Routes {
    const val ONBOARDING = "onboarding"
    const val CULLING = "culling"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val cullingViewModel: CullingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.settings.collectAsState()
            val loaded = settings

            FidFanCullrTheme(
                accent = loaded?.accentPalette ?: com.fidfanstudios.fidfancullr.data.AccentPalette.VIOLET_PIXEL,
                themeMode = loaded?.themeMode ?: com.fidfanstudios.fidfancullr.data.ThemeMode.SYSTEM,
                useDynamicColor = loaded?.useDynamicColor ?: true,
                pureBlack = loaded?.pureBlackTheme ?: false
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (loaded == null) {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    } else {
                        AppNavHost(
                            startDestination = if (loaded.onboardingComplete) Routes.CULLING else Routes.ONBOARDING,
                            settingsViewModel = settingsViewModel,
                            cullingViewModel = cullingViewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    startDestination: String,
    settingsViewModel: SettingsViewModel,
    cullingViewModel: CullingViewModel
) {
    val navController = rememberNavController()

    // Onboarding -> Culling is an explicit hard cut (no crossfade): the old
    // screen fully disappears before the new one draws, instead of both
    // being visible mid-transition (which showed the welcome screen
    // blended under the culling image for a moment). Settings still gets a
    // normal fade since it's a lightweight overlay-style screen where a
    // brief crossfade doesn't read as a glitch.
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { fadeIn(tween(180)) + slideInHorizontally(initialOffsetX = { it / 10 }, animationSpec = tween(220)) },
        exitTransition = { fadeOut(tween(150)) + slideOutHorizontally(targetOffsetX = { -it / 12 }, animationSpec = tween(180)) }
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                viewModel = settingsViewModel,
                onFinished = {
                    navController.navigate(Routes.CULLING) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CULLING) {
            CullingScreen(
                viewModel = cullingViewModel,
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(
            Routes.SETTINGS,
            enterTransition = { fadeIn(tween(180)) + slideInHorizontally(initialOffsetX = { it / 8 }, animationSpec = tween(220)) },
            exitTransition = { fadeOut(tween(150)) + slideOutHorizontally(targetOffsetX = { it / 10 }, animationSpec = tween(180)) }
        ) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
