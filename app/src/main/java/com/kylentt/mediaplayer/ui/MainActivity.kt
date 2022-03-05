package com.kylentt.mediaplayer.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_ANDROID
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_COLOROS_FM
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_DRIVE_LEGACY
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_EXTERNAL_STORAGE
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.domain.presenter.util.State.ServiceState
import com.kylentt.mediaplayer.ui.screen.*
import com.kylentt.mediaplayer.ui.screen.landing.PermissionScreen
import com.kylentt.mediaplayer.ui.screen.main.MainScreen
import com.kylentt.mediaplayer.ui.theme.md3.MaterialTheme3
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@ExperimentalPermissionsApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val TAG: String = this::class.java.simpleName
        var isActive = false
    }

    private val controllerVM: ControllerViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isActive = true

        // Connect to the Service then handle Intent that opens this App
        // This should be fine since onNewIntent is not called when Activity is first created
        onNewIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        // I know its still unstable but sometimes its stop checking midway.
        installSplashScreen()
            .apply {
                setKeepOnScreenCondition {
                    Timber.d("MainActivity KeepOnScreenCondition ${controllerVM.serviceState.value}")
                    when (controllerVM.serviceState.value) {
                        is ServiceState.Disconnected, is ServiceState.Connecting -> true
                        else -> false
                    }
                }

                /* TODO: setOnExitAnimationListener { } */
            }

        setContent {
            MaterialTheme3 {
                Root()
            }
        }
    }

    @Composable
    fun Root() {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RootNavigation()
        }
    }

    @Composable
    fun RootNavigation() {
        val rootNavController = rememberNavController()

        NavHost(
            navController = rootNavController,
            startDestination = Main_route,
            route = Root_route
        ) {
            // TODO : landingNavGraph
            mainNavGraph(navController = rootNavController)
            extraNavGraph(navController = rootNavController)
        }
    }

    // Navigation that have the Bottom Navigation Bar.
    // basically the NavGraph that have the Screen in which it display the Main Functionality of this App
    fun NavGraphBuilder.mainNavGraph(
        navController: NavHostController
    ) {
        navigation(
            startDestination = Screen.MainScreen.route,
            route = Main_route
        ) {
            composable(Screen.MainScreen.route) {
                MainScreen(rootController = navController)
            }
        }
    }

    // extra's composable which is basically the old `Permission_Activity` or `Settings_Activity`
    // or them as Fragment in which is navigate-able globally
    fun NavGraphBuilder.extraNavGraph(
        navController: NavController
    ) {
        navigation(
            startDestination = Screen.PermissionScreen.route,
            route = Extra_route
        ) {
            composable(Screen.PermissionScreen.route) {
                PermissionScreen(navController)
            }
            composable(Screen.SettingsScreen.route) {
                // TODO: Settings Screen
            }
        }
    }

    // Handle Incoming Intent such as ACTION_VIEW for Audio Files
    // This Function Only called when MainActivity is not Destroyed
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Timber.d("IntentHandler MainActivity NewIntent $intent")
        intent?.let {
            Timber.d("IntentHandler MainActivity forwarding Intent ${intent.action}")
            handleIntent(intent)
        }
    }

    // check the requested action of the Intent
    // maybe make class for Intent request
    private fun handleIntent(intent: Intent) {
        controllerVM.connectService(
            onConnected = {
                when (intent.action) {
                    Intent.ACTION_VIEW -> handleIntentActionView(intent)
                }
            }
        )
    }

    // check for Data Availability then forward it to ControllerVM
    private fun handleIntentActionView(intent: Intent) {
        Timber.d("IntentHandler MainActivity IntentActionView")

        intent.data?.let { uri ->
            Timber.d("IntentHandler MainActivity handling uri scheme ${uri.scheme} from $uri")

            val uris = uri.toString()
            when (uri.scheme) {
                "content" -> {
                    when {
                        uris.startsWith(PROVIDER_ANDROID) -> {
                            lifecycleScope.launch { controllerVM.handleDocsIntent(uri) }
                        }
                        uris.startsWith(PROVIDER_DRIVE_LEGACY) -> {
                            lifecycleScope.launch { controllerVM.handleItemIntent(uri) }
                        }
                        uris.startsWith(PROVIDER_EXTERNAL_STORAGE) -> {
                            lifecycleScope.launch { controllerVM.handleDocsIntent(uri) }
                        }
                        uris.startsWith(PROVIDER_COLOROS_FM) -> {
                            lifecycleScope.launch { controllerVM.handleDocsIntent(uri) }
                        }
                        else -> {
                            lifecycleScope.launch { controllerVM.handleDocsIntent(uri) }
                            Toast.makeText(this, "unsupported, please inform us \n " + uris.split("/")[2], Toast.LENGTH_LONG).show()
                        }
                    }
                }
                else -> {
                    Toast.makeText(this, "unsupported, please inform us \n" + uris.split("/")[2], Toast.LENGTH_LONG).show()
                }
            }
        } ?: Timber.e("IntentHandler ActionView null Data")
    }

    override fun onDestroy() {
        isActive = false
        super.onDestroy()
    }
}

