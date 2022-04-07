package com.kylentt.musicplayer.ui.musicactivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.kylentt.musicplayer.core.helper.PermissionHelper
import com.kylentt.musicplayer.core.helper.UIHelper.disableFitWindow
import com.kylentt.musicplayer.domain.MediaViewModel
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceState
import com.kylentt.musicplayer.ui.musicactivity.Hidden.checkLauncherIntent
import com.kylentt.musicplayer.ui.musicactivity.compose.MusicComposeDefault
import com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3.MaterialTheme3
import com.kylentt.musicplayer.ui.preferences.AppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

    private val mediaVM: MediaViewModel by viewModels()

    private val intentHolder: IntentWrapper = IntentWrapper.EMPTY
    private val pendingGranted = mutableStateListOf<() -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkLauncherIntent(intent)
        intentHolder.updateIntent(intent ?: IntentWrapper.EMPTY.getOriginalIntent())
        savedInstanceState?.let { validateCreation(it) }
            ?: run {
                if (mediaVM.shouldHandleIntent == false) {
                    intentHolder.handled = true
                }
            }

        pendingGranted.add { handleIntent(intentHolder) }

        disableFitWindow()
        installSplashScreen()
            .setKeepOnScreenCondition {
                when (mediaVM.serviceState.value) {
                    MediaServiceState.CONNECTED, is MediaServiceState.ERROR -> {
                        mediaVM.appState.value == AppState.Defaults.INVALID
                    }
                    else -> true
                }
            }

        setContent {

            MaterialTheme3 {

                MusicComposeDefault {
                    val mPendingGranted = remember { mediaVM.pendingGranted } ; mPendingGranted.forEach { it() } ; mPendingGranted.clear()
                    val pendingGranted = remember { this.pendingGranted } ; pendingGranted.forEach { it() } ; pendingGranted.clear()

                }
            }
        }
    }

    private fun validateCreation(savedInstanceState: Bundle) {
        Timber.d("MainActivity savedInstanceState ValidateCreation ${mediaVM.serviceState.value}")
        if (mediaVM.serviceState.value is MediaServiceState.UNIT) {
            intentHolder.handled = true
        }
    }

    override fun onStart() {
        super.onStart()
        mediaVM.connectService()
        isActive = true
    }

    // Consider Make ReceiverActivity for Specific Mime Type for better uri recognition
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("onNewIntent $intent, type = ${intent?.type}, scheme = ${intent?.scheme}")
        if (intent != null) {
            this.intent = intent
            intentHolder.updateIntent(intent)
            Timber.d("MainActivity updated Intent $intentHolder, pending = ${pendingGranted.size}")
            if (PermissionHelper.checkStoragePermission(this)) {
                handleIntent(intentHolder)
            } else {
                Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun handleIntent(wrapped: IntentWrapper) = with(wrapped) {
        if (handled || action == null) {
            Timber.d("MainActivity HandleIntent $wrapped is either handled or have null action")
            return@with
        }
        lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                val(data: Unit, time: Duration) = measureTimedValue { mediaVM.handleIntent(wrapped.copy()) }
                Timber.d("MainActivity HandledIntent with ${time.inWholeMilliseconds}ms")
                wrapped.getOriginalIntent().action = null
                wrapped.handled = true
            }
        }
    }

    override fun onDestroy() {
        pendingGranted.clear()
        isActive = false
        super.onDestroy()
        Timber.d("MainActivity cleared pendingGranted = ${mediaVM.pendingGranted.size}")
        Timber.d("onDestroy")
    }

    companion object {
        var isActive = false
            private set
    }
}

// Cloning is an option but I like it to be more explicit
data class IntentWrapper (
    var action: String?,
    var data: Uri?,
    var scheme: String?,
    var type: String?,
    var updatedIntent: Intent,
    private var originalIntent: Intent, // consider Nullable Instead
    var handled: Boolean = false
) {

    override fun toString(): String {
        return "${this.hashCode()} " + "\nIntentWrapper(action = $action, data = $data, scheme = $scheme,intent = $updatedIntent OriIntent = $originalIntent, handled = $handled)"
    }

    fun updateIntent(
        intent: Intent,
        handled: Boolean = intent.action == null
    ) {
        if (this.originalIntent == EMPTY.originalIntent) { this.originalIntent = intent }

        this.action = intent.action
        this.data = intent.data
        this.scheme = intent.scheme
        this.type = intent.type
        this.updatedIntent = intent
        this.handled = handled
    }

    fun getOriginalIntent() = this.originalIntent

    companion object {
        val EMPTY = fromIntent(Intent())

        fun fromIntent(intent: Intent) = with(intent) { toWrapper() }
        fun Intent.toWrapper() = IntentWrapper(
            action = action,
            data = data,
            scheme = scheme,
            type = type,
            updatedIntent = this,
            originalIntent = this,
            handled = action == null
        )
    }
}

