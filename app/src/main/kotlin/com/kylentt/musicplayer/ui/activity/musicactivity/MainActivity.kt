package com.kylentt.musicplayer.ui.activity.musicactivity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.IntDef
import androidx.compose.runtime.remember
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.theme.MaterialDesign3Theme
import com.kylentt.musicplayer.core.helper.UIHelper.disableFitWindow
import com.kylentt.musicplayer.domain.MediaViewModel
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.musicplayer.domain.mediasession.service.MediaServiceState
import com.kylentt.musicplayer.ui.activity.helper.IntentWrapper
import com.kylentt.musicplayer.ui.activity.helper.IntentWrapper.Companion.wrap
import com.kylentt.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.Alive
import com.kylentt.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.Destroyed
import com.kylentt.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.Ready
import com.kylentt.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.Visible
import com.kylentt.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.setCurrentHash
import com.kylentt.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.setCurrentState
import com.kylentt.musicplayer.ui.activity.musicactivity.acompose.MusicComposeDefault
import com.kylentt.musicplayer.ui.helper.AppToaster
import com.kylentt.mediaplayer.app.settings.AppSettings
import com.kylentt.musicplayer.ui.preferences.AppState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

  private val mediaVM: MediaViewModel by viewModels()

  @Inject
  lateinit var appScope: AppScope

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkLauncherIntent()
    setupExtra()

    Timber.d("MainActivity onCreate ${mediaVM.pendingGranted.size}")

    setContent {
      MaterialDesign3Theme {
        MusicComposeDefault {
          val mPendingGranted = remember { mediaVM.pendingGranted }
          mPendingGranted.syncEachClear()
        }
      }
    }
  }

  private fun setupExtra() {
    disableFitWindow()
    setCurrentHash()
    setCurrentState(Alive)
  }

  override fun onStart() {
    Timber.d("MainActivity onStart")
    mediaVM.connectService()
    setCurrentState(Visible)
    return super.onStart()
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    Timber.d("MainActivity onPostCreate")
    return super.onPostCreate(savedInstanceState)
  }

  override fun onResume() {
    Timber.d("MainActivity onResume")
    setCurrentState(Ready)
    return super.onResume()
  }

  override fun onNewIntent(intent: Intent?) {
    Timber.d("MainActivity onNewIntent $intent, type = ${intent?.type}, scheme = ${intent?.scheme}")
    intent?.let {
      require(IntentValidator.hasKey(it))
      handleIntent(it.wrap())
    }
    return run {
      Timber.d("onNewIntent returning with " +
        "\npendingGranted = ${mediaVM.pendingGranted.size}")
      super.onNewIntent(intent)
    }
  }

  override fun onStop() {
    Timber.d("onStop")
    setCurrentState(Alive)
    return super.onStop()
  }

  override fun onDestroy() {
    Timber.d("onDestroy")
    setCurrentState(Destroyed)
    return super.onDestroy()
  }



  private fun checkLauncherIntent() {
    requireNotNull(intent)
    check(intent.action == Defaults.intentAction)
    installSplashScreen()
  }

  private fun isRecreated(savedInstanceState: Bundle?) = savedInstanceState != null

  private fun keepScreenCondition(): Boolean = listOf(
    mediaVM.serviceState.value is MediaServiceState.UNIT,
    mediaVM.serviceState.value is MediaServiceState.CONNECTING,
    mediaVM.appSettings.value == AppSettings.INVALID
  ).any { it }

  @OptIn(ExperimentalTime::class)
  private fun handleIntent(wrapped: IntentWrapper) {
    verifyMainThread()
    if (!wrapped.shouldHandleIntent()) {
      Timber.d("MainActivity HandleIntent $wrapped is either handled or have null required properties")
      return
    }
    if (wrapped.isActionView()) {
      if (!AppDelegate.hasStoragePermission) {
        with(mediaVM.pendingNewIntent) {
          if (isEmpty()) {
            add(wrapped)
            mediaVM.pendingGranted.add {
              forEach { handleIntent(it) }
              clear()
            }
          } else {
            removeAll { it.isActionView() }
            add(wrapped)
          }
        }
        appScope.mainScope.launch {
          AppToaster.blockIfSameToasting("Storage Permission Needed", true)
        }
        return
      }
    }
    appScope.ioScope.launch {
      val (_: Unit, time: Duration) = measureTimedValue { mediaVM.handleIntent(wrapped) }
      Timber.d("MainActivity HandledIntent with ${time.inWholeMilliseconds}ms")
      wrapped.markHandled()
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

    val wasLaunched
      get() = Lifecycle.wasLaunched
    val isDestroyed
      get() = !Lifecycle.isAlive && wasLaunched
    val isAlive
      get() = Lifecycle.isAlive
    val isVisible
      get() = Lifecycle.isVisible
    val isReady
      get() = Lifecycle.isReady

    val activityStateStr
      get() = Lifecycle.getStateStr

    fun startActivity(
      launcher: Activity,
      intent: Intent? = null
    ) {
      if (!isAlive) {
        launcher.startActivity(Defaults.getDefaultIntent(launcher))
      }
      if (intent != null) {
        intent.apply { setClass(launcher, MainActivity::class.java) }
        return launcher.startActivity(intent)
      }
    }

    private object Defaults {
      const val intentAction = Intent.ACTION_MAIN
      fun getDefaultIntent(context: Context) = Intent()
        .apply {
          action = intentAction
          setClass(context, MainActivity::class.java)
        }
    }

    private object Lifecycle {

      @Retention(AnnotationRetention.SOURCE)
      @Target(
        AnnotationTarget.FIELD,
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER,
        AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.LOCAL_VARIABLE,
      )
      @IntDef(Nothing, Destroyed, Alive, Visible, Ready)
      annotation class ActivityState

      const val Nothing = 0 // Not Launched in any way
      const val Destroyed = 1 // onDestroy() was called
      const val Alive = 2 // onCreate() or onStop() was called
      const val Visible = 3 // onStart() or onPause() was called
      const val Ready = 4 // onResume() was called

      private var currentActivityHash = Nothing
      private var currentActivityState = Nothing

      val getStateStr
        get() = toActivityStateStr(currentActivityState)

      val wasLaunched
        get() = currentActivityState > Nothing
          && currentActivityHash != Nothing
      val isAlive
        get() = currentActivityState > Destroyed
      val isVisible
        get() = currentActivityState > Alive
      val isReady
        get() = currentActivityState > Visible

      fun toActivityStateStr(@ActivityState int: Int) =
        when(currentActivityState) {
          Nothing -> "Not Launched"
          Destroyed -> "Destroyed"
          Alive -> "Alive"
          Visible -> "Visible"
          Ready -> "Ready"
          else -> "INVALID"
        }

      fun MainActivity.setCurrentHash() {
        currentActivityHash = this.hashCode()
      }

      private fun MainActivity.checkCurrentHash(): Boolean {
        val thisHash = this.hashCode()
        val thatHash = currentActivityHash
        return thisHash == thatHash
      }

      fun MainActivity.setCurrentState(@ActivityState state: Int) {
        if (state == Destroyed) {
          if (!checkCurrentHash()) {
            return Timber.e("MainActivity onDestroy ${this.hashCode()} was Called Late")
          }
        }
        check(checkCurrentHash())
        currentActivityState = state
      }

    }

  }


}
