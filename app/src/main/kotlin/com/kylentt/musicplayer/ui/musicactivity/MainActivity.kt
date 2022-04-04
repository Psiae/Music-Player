package com.kylentt.musicplayer.ui.musicactivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.kylentt.musicplayer.core.helper.PermissionHelper
import com.kylentt.musicplayer.core.helper.UIHelper.disableFitWindow
import com.kylentt.musicplayer.core.helper.elseNull
import com.kylentt.musicplayer.domain.MediaViewModel
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceState
import com.kylentt.musicplayer.ui.musicactivity.compose.MusicComposeDefault
import com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3.MaterialTheme3
import com.kylentt.musicplayer.ui.preferences.AppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

    private val mediaVM: MediaViewModel by viewModels()

    private val intentHolder: IntentWrapper = IntentWrapper.EMPTY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intentHolder.updateIntent(intent ?: IntentWrapper.EMPTY.intent)

        savedInstanceState?.let { validateCreation(it) }
            ?: run {
                elseNull(mediaVM.shouldHandleIntent == false) { intentHolder.action = null }
                    ?: mediaVM.whenGranted.add { handleIntent(intentHolder) }

                lifecycleScope.launch {
                    val state = mediaVM.appState.first { it != AppState.Defaults.INVALID }
                    mediaVM.navStartIndex.value = state.navigationIndex
                }
            }

        disableFitWindow()
        installSplashScreen()
            .setKeepOnScreenCondition { mediaVM.showSplashScreen }

        setContent {

            MaterialTheme3 {

                MusicComposeDefault {
                    val whenGranted = remember { mediaVM.whenGranted }
                    whenGranted.forEach { it() }
                    whenGranted.clear()
                }
            }
        }
    }

    private fun validateCreation(savedInstanceState: Bundle) {
        Timber.d("savedInstanceState ValidateCreation ${mediaVM.serviceState.value}")
        if (mediaVM.serviceState.value is MediaServiceState.UNIT) {
            intentHolder.handled = true
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
            intentHolder.updateIntent(intent)
            if (PermissionHelper.checkStoragePermission()) {
                handleIntent(intentHolder)
            } else {
                Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleIntent(wrapped: IntentWrapper) = with(wrapped) {
        if (handled) return@with
        val intent = wrapped.copy()
        lifecycleScope.launch { withContext(Dispatchers.Default) {
            mediaVM.handleIntent(intent)
            wrapped.handled = true
        } }
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

// Cloning is an option but I like it to be more explicit
data class IntentWrapper (
    var action: String?,
    var data: Uri?,
    var scheme: String?,
    val intent: Intent,
    var handled: Boolean = false
) {

    override fun toString(): String {
        return "${this.hashCode()} " + "IntentWrapper(action = $action, data = $data, scheme = $scheme, intent = $intent, handled = $handled)"
    }

    fun updateIntent(
        intent: Intent,
        handled: Boolean = intent.action == null
    ) {
        this.action = intent.action
        this.data = intent.data
        this.scheme = intent.scheme
        this.handled = handled
    }

    companion object {
        val EMPTY = fromIntent(Intent())

        fun fromIntent(intent: Intent) = with(intent) { toWrapper() }
        fun Intent.toWrapper() = IntentWrapper(
            action = action,
            data = data,
            scheme = scheme,
            intent = this,
            handled = action == null
        )
    }
}

