package com.kylentt.mediaplayer.ui.mainactivity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_ANDROID
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_COLOROS_FM
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_DRIVE_LEGACY
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_EXTERNAL_STORAGE
import com.kylentt.mediaplayer.disposed.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.disposed.domain.presenter.util.State.ServiceState
import com.kylentt.mediaplayer.domain.mediaSession.MediaViewModel
import com.kylentt.mediaplayer.ui.mainactivity.compose.Root
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.util.ComposePermission
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.util.RequirePermission
import com.kylentt.mediaplayer.ui.mainactivity.compose.theme.md3.MaterialTheme3
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val controllerVM: ControllerViewModel by viewModels()
    private val mediaVM: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isActive = true
        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen()
            .apply {
                setKeepOnScreenCondition {
                    Timber.d("MainActivity KeepOnScreenCondition ${controllerVM.serviceState.value}")
                    when (controllerVM.serviceState.value) {
                        is ServiceState.Disconnected -> {
                            controllerVM.connectService()
                            true
                        }
                        is ServiceState.Connecting -> true
                        else -> false
                    }
                }
                // TODO: setOnExitAnimationListener { }
            }

        setContent {
            Timber.d("ComposeDebug setContent")
            MaterialTheme3 {
                RequirePermission(
                    perms = ComposePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        onGranted = {
                            Root()
                            onNewIntent(intent)
                        },
                        onNotDenied = {
                            Root()
                        },
                        onDenied = {
                            Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show()
                        },
                        onPermissionScreenRequest = {
                            val i = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult(),
                                onResult = {}
                            )
                            SideEffect {
                                i.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${packageName}".toUri()))
                            }
                        }
                    )
                )
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

    private fun handleIntent(intent: Intent) {
        controllerVM.connectService(
            onConnected = {
                when (intent.action) {
                    Intent.ACTION_VIEW -> handleIntentActionView(intent)
                }
                intent.action = null
            }
        )
    }

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
        super.onDestroy()
        isActive = false
    }

    companion object {
        var isActive = false
    }
}

