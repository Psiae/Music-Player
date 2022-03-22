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
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val controllerVM: ControllerViewModel by viewModels()
    private val mediaVM: MediaViewModel by viewModels()

    private var newIntentInterceptor: ((Intent) -> Unit)? = null

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreateCheckSavedInstanceState $savedInstanceState ${this.intent}")

        // Don't want to deal with weird foreground service behavior
        validateInstanceState(savedInstanceState)

        isActive = true
        WindowCompat.setDecorFitsSystemWindows(window, false)

        installSplashScreen()
            .apply {
                setKeepOnScreenCondition {
                    Timber.d("MainActivity KeepOnScreenCondition ${controllerVM.serviceState.value}")
                    when (controllerVM.serviceState.value) {
                        is ServiceState.Unit -> {
                            controllerVM.connectService()
                            true
                        }
                        is ServiceState.Connected -> false
                        else -> newIntentInterceptor != null
                    }
                }
                // TODO: setOnExitAnimationListener { }
            }
        setContent {
            Timber.d("ComposeDebug setContent")
            MaterialTheme3 {
                if (newIntentInterceptor == null) {
                    RequirePermission(permission = ComposePermission.SinglePermission(
                        permissionStr = Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        onGranted = {
                            Root()
                            onNewIntent(intent)
                        },
                        onNotDenied = {
                            FakeRoot()
                        },
                        onDenied = {
                            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                            val i =
                                rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {}
                            PermissionScreen(grantButtonText = "Grant Storage Permission") {
                                if (it.status.shouldShowRationale) {
                                    it.launchPermissionRequest()
                                } else {
                                    i.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        "package:${packageName}".toUri()))
                                }
                            }
                        },
                        onGrantedAfterRequest = {
                            startActivity(run {
                                finish()
                                intent.action?.let { intent } ?: Intent(this,
                                    MainActivity::class.java)
                            })
                            exitProcess(0)
                        },
                    ))
                }
            }
        }
    }

    private fun validateInstanceState(savedInstanceState: Bundle?) {
        Timber.d("validateInstanceState Intent $intent ${intent.action} ${controllerVM.serviceState.value}")
        if (savedInstanceState != null) {
            Timber.d("savedInstanceState != null")
            // Activity is Recreated
            if (controllerVM.serviceState.value is ServiceState.Unit) {
                Timber.d("savedInstanceState serviceState is unit")
                // Update Intent Interceptor if Service is Unit
                // so Config Changes such as Device rotation will not change it
                newIntentInterceptor = { it ->
                    startActivity(run {
                        finish()
                        if (it == this.intent) {
                            Intent(this@MainActivity, MainActivity::class.java)
                        } else {
                            it
                        }
                    })
                    exitProcess(0)
                }
                lifecycleScope.launch {
                    delay(1000)
                    newIntentInterceptor?.let {
                        it(Intent(this@MainActivity, MainActivity::class.java))
                    }
                }
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
                    handleIntent(this.intent)
                } else {
                    Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show()
                }
            }
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
        Timber.d("onDestroy")
        isActive = false
    }

    companion object {
        var isActive = false
    }
}

