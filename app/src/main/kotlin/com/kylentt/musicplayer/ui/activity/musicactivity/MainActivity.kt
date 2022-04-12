package com.kylentt.musicplayer.ui.activity.musicactivity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.kylentt.musicplayer.core.helper.PermissionHelper.checkStoragePermission
import com.kylentt.musicplayer.core.helper.UIHelper.disableFitWindow
import com.kylentt.musicplayer.domain.MediaViewModel
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceState
import com.kylentt.musicplayer.ui.activity.helper.IntentWrapper
import com.kylentt.musicplayer.ui.activity.helper.IntentWrapper.Companion.EMPTY
import com.kylentt.musicplayer.ui.activity.helper.IntentWrapper.Companion.isEmpty
import com.kylentt.musicplayer.ui.activity.helper.IntentWrapper.Companion.wrap
import com.kylentt.musicplayer.ui.activity.musicactivity.compose.MusicComposeDefault
import com.kylentt.musicplayer.ui.activity.musicactivity.compose.theme.md3.MaterialTheme3
import com.kylentt.musicplayer.ui.preferences.AppSettings
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
  private val pendingGranted = mutableStateListOf<() -> Unit>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkLauncherIntent()
    disableFitWindow()
    installSplashScreen()
      .setKeepOnScreenCondition { keepScreenCondition() }
    setContent {
      MaterialTheme3 {
        MusicComposeDefault {
          val mPendingGranted = remember { mediaVM.pendingGranted }
          val pendingGranted = remember { pendingGranted }
          mPendingGranted.syncEachClear()
          pendingGranted.syncEachClear()
        }
      }
    }
  }

  override fun onStart() {
    mediaVM.connectService()
    setVisible()
    return run {
      Timber.d("MainActivity onStart")
      super.onStart()
    }
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    if (!pendingNewIntent.isEmpty()) {
      val intent = pendingNewIntent.getIntent()
      pendingNewIntent = EMPTY
      onNewIntent(intent)
    }
    return run {
      Timber.d("MainActivity onPostCreate")
      super.onPostCreate(savedInstanceState)
    }
  }

  override fun onNewIntent(intent: Intent?) {
    Timber.d("onNewIntent $intent, type = ${intent?.type}, scheme = ${intent?.scheme}")
    intent?.let {
      val wrapped = it.wrap()
      when {
        wrapped.isActionView() -> {
          if (checkStoragePermission()) {
            handleIntent(wrapped)
          } else {
            if (pendingNewIntent.isEmpty()) {
              pendingNewIntent = wrapped
              pendingGranted.add {
                handleIntent(pendingNewIntent)
                pendingNewIntent = EMPTY
              }
            } else {
              pendingNewIntent = wrapped
            }
            Toast.makeText(this, "Storage Permission Needed", Toast.LENGTH_LONG).show()
          }
        }
      }
    }
    return run {
      Timber.d("onNewIntent returning with \npending = $pendingNewIntent \npendingGranted = ${pendingGranted.size}")
      super.onNewIntent(intent)
    }
  }

  override fun onStop() {
    Timber.d("onStop")
    setAlive()
    return super.onStop()
  }

  override fun onDestroy() {
    Timber.d("onDestroy")
    setNoActivity()
    return super.onDestroy()
  }

  private fun checkLauncherIntent() {
    requireNotNull(intent)
    check(intent.action == Intent.ACTION_MAIN)
    setCurrentActivity()
  }

  private fun isRecreated(savedInstanceState: Bundle?) = savedInstanceState != null
  private fun isConnectAttempted() =
    MediaServiceState.isConnectAttempted(mediaVM.serviceState.value)

  private fun shouldHandleLauncherIntent(savedInstanceState: Bundle?): Boolean {
    if (!isConnectAttempted()) {
      if (isRecreated(savedInstanceState)) return false
      if (mediaVM.getConnectedStateHandle) return false
    }
    return true
  }

  private fun keepScreenCondition(): Boolean = listOf(
    when (mediaVM.serviceState.value) {
      is MediaServiceState.UNIT, is MediaServiceState.CONNECTING -> true; else -> false
    },
    mediaVM.appState.value == AppState.Defaults.INVALID,
    mediaVM.appSettings.value == AppSettings.Defaults.INVALID
  ).any { it }

  @OptIn(ExperimentalTime::class)
  private fun handleIntent(wrapped: IntentWrapper) {
    if (!wrapped.shouldHandleIntent()) {
      Timber.d("MainActivity HandleIntent $wrapped is either handled or have null required properties")
      return
    }
    lifecycleScope.launch {
      withContext(Dispatchers.Default) {
        val (_: Unit, time: Duration) = measureTimedValue { mediaVM.handleIntent(wrapped.copy()) }
        Timber.d("MainActivity HandledIntent with ${time.inWholeMilliseconds}ms")
        wrapped.markHandled()
      }
    }
  }

  private fun MutableList<() -> Unit>.syncEachClear(lock: Any = this) {
    synchronized(lock) {
      forEach { it() }
      clear()
    }
  }

  // TODO: Expose Activity state
  companion object {
    private const val noActivity = 1
    private const val AliveInt = 2
    private const val VisibleInt = 3

    private var currentActivityHash: Int = noActivity
    private var currentActivityState: Int = noActivity
    private var pendingNewIntent: IntentWrapper = EMPTY

    val isAlive
      get() = currentActivityState >= AliveInt
    val isVisible
      get() = currentActivityState >= VisibleInt

    private fun MainActivity.setCurrentActivity() {
      Timber.d("ActivityHash $currentActivityHash changed to ${this.hashCode()}")
      currentActivityHash = this.hashCode()
      setAlive()
    }

    private fun MainActivity.setNoActivity() {
      val thisHash = this.hashCode()
      val thatHash = currentActivityHash
      if (thisHash != thatHash) {
        return Timber.d("ActivityHash notEquals, ignored, \nthis = ${thisHash}\ncurrent = $thatHash")
      }
      currentActivityState = noActivity
      currentActivityHash = noActivity
      return Timber.d("ActivityHash Equals, changed to -1, \nthis = ${thisHash}\nthat = ${thatHash}\nto = $currentActivityState ")
    }

    private fun MainActivity.setAlive() {
      val thisHash = this.hashCode()
      val thatHash = currentActivityHash
      if (thisHash != thatHash) {
        return Timber.d("ActivityHash notEquals, ignored, \nthis = ${thisHash}\ncurrent = $thatHash")
      }
      currentActivityState = AliveInt
      return Timber.d("ActivityHash Equals, changed to -1, \nthis = ${thisHash}\nthat = ${thatHash}\nto = $currentActivityState ")
    }

    private fun MainActivity.setVisible() {
      val thisHash = this.hashCode()
      val thatHash = currentActivityHash
      if (thisHash != thatHash) {
        return Timber.d("ActivityHash notEquals, ignored, \nthis = ${thisHash}\ncurrent = $thatHash")
      }
      currentActivityState = VisibleInt
      return Timber.d("ActivityHash Equals, changed to -1, \nthis = ${thisHash}\nthat = ${thatHash}\nto = $currentActivityState ")
    }
  }
}
