package com.kylentt.musicplayer.ui.musicactivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_DRIVE_LEGACY
import com.kylentt.mediaplayer.disposed.domain.presenter.ControllerViewModel
import com.kylentt.musicplayer.core.helper.PermissionHelper
import com.kylentt.musicplayer.core.helper.UIHelper.disableFitWindow
import com.kylentt.musicplayer.core.helper.elseNull
import com.kylentt.musicplayer.core.helper.orNull
import com.kylentt.musicplayer.domain.MediaViewModel
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceState
import com.kylentt.musicplayer.ui.musicactivity.compose.MusicComposeDefault
import com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3.MaterialTheme3
import com.kylentt.musicplayer.ui.preferences.AppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

    private val controllerVM: ControllerViewModel by viewModels()
    private val mediaVM: MediaViewModel by viewModels()

    private val whenGranted = mutableListOf< () -> Unit >()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        savedInstanceState?.let { validateCreation(it) }
            ?: run {
                elseNull(mediaVM.shouldHandleIntent == false) { this.intent.action = null }
                    ?: whenGranted.add { handleIntent(this.intent) }

                lifecycleScope.launch {
                    val state = mediaVM.appState.first { it != AppState.Defaults.invalidState }
                    mediaVM.navigationStartIndex = state.navigationIndex
                }
            }

        disableFitWindow()
        installSplashScreen()
            .setKeepOnScreenCondition { mediaVM.showSplashScreen }
        setContent {
            MaterialTheme3 {
                MusicComposeDefault(
                    whenGranted = {
                        whenGranted.forEach { it() }
                        whenGranted.clear()
                    }
                )
            }
        }
    }

    private fun validateCreation(savedInstanceState: Bundle) {
        Timber.d("savedInstanceState ValidateCreation ${mediaVM.serviceState.value}")
        if (mediaVM.serviceState.value is MediaServiceState.UNIT) {
            this.intent.action = null
        }
    }

    override fun onStart() {
        super.onStart()
        mediaVM.connectService()
        isActive = true
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
        when (intent.action) {
            Intent.ACTION_VIEW -> handleIntentActionView(intent)
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
        } ?: Timber.w("IntentHandler ActionView null Data")
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
// however I don't like having it in domain layer, should only be modifiable here
data class IntentWrapper (
    val action: String?,
    val data: Uri?,
    val scheme: String?,
    val intent: Intent
) {
    companion object {
        fun fromIntent(intent: Intent) = with(intent) { wrapIt() }
        private fun Intent.wrapIt() = IntentWrapper(action, data, scheme, this.clone() as Intent)
    }
}

