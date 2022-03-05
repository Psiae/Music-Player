package com.kylentt.mediaplayer.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_ANDROID
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_COLOROS_FM
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_DRIVE_LEGACY
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_EXTERNAL_STORAGE
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.domain.presenter.util.State.ServiceState
import com.kylentt.mediaplayer.ui.theme.md3.MaterialTheme3
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.transformers.coil.CropSquareTransformation
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val TAG: String = this::class.java.simpleName
        var isActive = false
    }

    @Inject
    lateinit var coil: ImageLoader

    private val controllerVM: ControllerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("InitDebug MainActivity onCreate")
        isActive = true

        // Connect to the Service then handle Intent that opens this App
        onNewIntent(intent)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        installSplashScreen()
            .apply {
                setKeepOnScreenCondition {
                    when (controllerVM.serviceState.value) {
                        is ServiceState.Disconnected, is ServiceState.Connecting -> true
                        else -> false
                    }
                }
                /* TODO: setOnExitAnimationListener { } */
                Timber.d("InitDebug MainActivity STATE SplashScreen")
            }

        val havePermission = checkPermission()
        setContent {
            MaterialTheme3 {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigation(
                        start = when {
                            !havePermission -> Screen.PermissionScreen.route
                            else -> Screen.HomeScreen.route
                        },
                        vm = controllerVM
                    ) {
                        // TODO something todo after navigation?
                    }
                }
            }
        }
    }

    private fun checkPermission(): Boolean {
        return true
    }

    // Handle Incoming Intent such as ACTION_VIEW for Audio Files
    // This Function Only called when MainActivity is not Destroyed
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Timber.d("IntentHandler MainActivity NewIntent")
        intent?.let {
            handleIntent(intent)
        }
    }

    // check the requested action of the Intent
    private fun handleIntent(intent: Intent) {
        Timber.d("IntentHandler MainActivity forwarding Intent ${intent.action}")
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
            Timber.d("IntentHandler MainActivity handling uri scheme ${uri.scheme}")

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

    private suspend fun Bitmap.squareWithCoil(): Bitmap? {
        val req = ImageRequest.Builder(this@MainActivity)
            .diskCachePolicy(CachePolicy.DISABLED)
            .transformations(CropSquareTransformation())
            .size(256)
            .scale(Scale.FILL)
            .data(this)
            .build()
        return ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }

    override fun onDestroy() {
        isActive = false
        super.onDestroy()
    }
}

