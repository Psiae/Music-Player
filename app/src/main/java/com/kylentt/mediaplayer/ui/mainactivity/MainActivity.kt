package com.kylentt.mediaplayer.ui.mainactivity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.shouldShowRationale
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_ANDROID
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_COLOROS_FM
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_DRIVE_LEGACY
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_EXTERNAL_STORAGE
import com.kylentt.mediaplayer.disposed.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.disposed.domain.presenter.util.State.ServiceState
import com.kylentt.mediaplayer.domain.mediaSession.MediaViewModel
import com.kylentt.mediaplayer.ui.mainactivity.compose.ComposePermission
import com.kylentt.mediaplayer.ui.mainactivity.compose.PermissionScreen
import com.kylentt.mediaplayer.ui.mainactivity.compose.RequirePermission
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.FakeRoot
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.Root
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.theme.md3.MaterialTheme3
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.system.exitProcess

@AndroidEntryPoint
@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {

    private val controllerVM: ControllerViewModel by viewModels()
    private val mediaVM: MediaViewModel by viewModels()

    private var newIntentInterceptor: ((Intent) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Don't want to deal with weird foreground service behavior
        Timber.d("onCreateCheckSavedInstanceState $savedInstanceState ${this.intent}")
        validateCreation(savedInstanceState)

        isActive = true
        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen().setKeepOnScreenCondition {
            Timber.d("keepOnScreenCondition ${controllerVM.serviceState.value}")
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
                is ServiceState.Connected -> {
                    newIntentInterceptor != null
                }
                else -> {
                    true
                }
            }
        }
        setContent {
            Timber.d("ComposeDebug setContent")
            MaterialTheme3 {
                if (newIntentInterceptor == null) {
                    RequirePermission(permission = ComposePermission.SinglePermission(permissionStr = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        onGranted = {
                            onNewIntent(intent)
                            Root()
                        },
                        onNotDenied = {
                            // For some reason this composable callback not executed more than once
                            // so permission request is defaulted in RP composable
                            FakeRoot()
                        },
                        onDenied = {
                            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                            val i = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {}
                            PermissionScreen(grantButtonText = "Grant Storage Permission") {
                                if (it.status.shouldShowRationale) {
                                    it.launchPermissionRequest()
                                } else {
                                    i.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        "package:${packageName}".toUri()))
                                }
                            }
                        },
                        onGrantedAfterDenied = {
                            startActivity(run {
                                finish()
                                intent.action?.let { intent } ?: Intent(this, MainActivity::class.java)
                            })
                            exitProcess(0)
                        },
                    ))
                }
            }
        }
    }

    private fun validateCreation(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (controllerVM.serviceState.value is ServiceState.Unit) {
                this.intent.action = null
            }
        }
    }

    // Change to ?let scope if needed
    // TODO: Move this to Handler class
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("IntentHandler MainActivity NewIntent $intent")
        if (intent != null) {
            if (newIntentInterceptor != null) {
                newIntentInterceptor!!.invoke(intent)
            } else {
                this.intent = intent
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("IntentHandler MainActivity NewIntent permission granted $intent")
                    handleIntent(this.intent)
                } else {
                    Timber.d("IntentHandler MainActivity NewIntent permission not granted $intent")
                    Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleIntent(intent: Intent) {
        controllerVM.connectService {
            when (intent.action) {
                Intent.ACTION_VIEW -> handleIntentActionView(intent)
            }
            intent.action = null
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

    private fun waitForNewIntentOrRestart(delay: Long = 1500) = lifecycleScope.launch {
        newIntentInterceptor = { it ->
            startActivity(run {
                finish()
                it
            })
            exitProcess(0)
        }
        delay(delay)
        newIntentInterceptor!!(Intent(this@MainActivity,
            MainActivity::class.java
        ))
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

