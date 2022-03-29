package com.kylentt.musicplayer.ui.musicactivity

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_DRIVE_LEGACY
import com.kylentt.mediaplayer.disposed.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.disposed.domain.presenter.util.State.ServiceState
import com.kylentt.mediaplayer.domain.mediaSession.MediaViewModel
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.FakeRoot
import com.kylentt.musicplayer.core.helper.PermissionHelper
import com.kylentt.musicplayer.core.helper.UIHelper.disableFitWindow
import com.kylentt.musicplayer.ui.musicactivity.compose.MusicCompose
import com.kylentt.musicplayer.ui.musicactivity.compose.environtment.ComposePermission
import com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3.MaterialTheme3
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalPermissionsApi::class)
@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

    private val controllerVM: ControllerViewModel by viewModels()
    private val mediaVM: MediaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Timber.d("onCreate withIntent, savedInstance $savedInstanceState  ${this.intent}")

        validateCreation(savedInstanceState)
        disableFitWindow()
        installSplashScreen().setKeepOnScreenCondition {
            when (controllerVM.serviceState.value) {
                is ServiceState.Unit -> {
                    controllerVM.connectService()
                    true
                }
                is ServiceState.Disconnected -> {
                    Timber.e("KeepOnScreenCondition ServiceDisconnected")
                    controllerVM.connectService()
                    true
                }
                is ServiceState.Connecting -> {
                    true
                }
                else -> {
                    false
                }
            }
        }

        setContent {

            MaterialTheme3 {

                // TODO() redo RequirePermission Composable

                val permissionResult = remember { mutableStateOf(false, policy = neverEqualPolicy()) }

                val permission = rememberPermissionState(permission = Manifest.permission.WRITE_EXTERNAL_STORAGE) { permissionResult.value = it }

                when {
                    permission.status.isGranted -> {
                        handleIntent(this.intent)
                        MusicCompose()
                    }
                    !permissionResult.value && permission.status.shouldShowRationale -> {
                        FakeRoot()
                        ComposePermission.SinglePermission.PermissionDefaults.OnNotDenied(persistent = true) { permission.launchPermissionRequest() }
                    }
                    !permissionResult.value && !permission.status.shouldShowRationale -> {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                        ComposePermission.SinglePermission.PermissionDefaults.OnDenied(grantButtonText = "Grant Storage Permission")
                    }
                    else -> {
                        Timber.e("permissionState should never reach here! ${permissionResult.value} ${permission.status}")
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isActive = true
    }

    private fun validateCreation(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (controllerVM.serviceState.value is ServiceState.Unit) {
                // Usually Process death and the activity is launched using supposedly handled intent
                // Consider Restarting JVM if there's impact in domain layer such as late initialization exception
                // Caused by System Trying to Force re-launch Foreground Service directly
                Timber.w("Invalid Creation Intent")
                this.intent.action = null
            }
        }
    }

    // Change to ?let scope if needed
    // TODO: Move this to Handler class
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent $intent")
        if (intent != null) {
            this.intent = intent
            if (PermissionHelper.checkStoragePermission()) {
                handleIntent(this.intent)
            } else {
                Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        controllerVM.connectService {
            when (intent.action) {
                Intent.ACTION_VIEW -> handleIntentActionView(intent)
            }
        }
    }

    private fun handleIntentActionView(intent: Intent) {
        Timber.d("IntentHandler MainActivity IntentActionView")

        intent.data?.let { uri ->
            Timber.d("IntentHandler MainActivity handling uri scheme ${uri.scheme} from $uri")

            val uris = uri.toString()
            when (uri.scheme) {
                "content" -> {
                    when {
                        uris.startsWith(PROVIDER_DRIVE_LEGACY) -> {
                            lifecycleScope.launch {
                                controllerVM.handleItemIntent(IntentWrapper.fromIntent(intent)) {
                                    intent.action = null
                                }
                            }
                        }
                        else -> lifecycleScope.launch {
                            controllerVM.handleDocsIntent(IntentWrapper.fromIntent(intent)) {
                                intent.action = null
                            }
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
        Timber.d("onDestroy")
    }

    companion object {
        var isActive = false
    }
}

// Passing Launcher Intent Around doesn't cause memory leak unless bound to class-in variable
// however I don't like having it in domain layer
data class IntentWrapper (
    val action: String?,
    val data: Uri?
) {
    companion object {
        fun fromIntent(intent: Intent) = with(intent) {
            IntentWrapper(action, data)
        }
    }
}

