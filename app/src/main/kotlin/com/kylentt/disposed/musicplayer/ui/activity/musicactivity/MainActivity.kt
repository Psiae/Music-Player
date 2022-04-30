package com.kylentt.disposed.musicplayer.ui.activity.musicactivity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.IntDef
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kylentt.mediaplayer.app.coroutines.AppScope
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate
import com.kylentt.mediaplayer.app.settings.AppSettings
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.theme.MaterialDesign3Theme
import com.kylentt.disposed.musicplayer.core.helper.UIHelper.disableFitWindow
import com.kylentt.disposed.musicplayer.domain.MediaViewModel
import com.kylentt.disposed.musicplayer.domain.mediasession.service.MediaServiceState
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.Alive
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.Destroyed
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.Ready
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.Visible
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.setCurrentHash
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.MainActivity.Companion.Lifecycle.setCurrentState
import com.kylentt.disposed.musicplayer.ui.activity.musicactivity.acompose.MusicComposeDefault
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class MainActivity : ComponentActivity() {

  @Inject
  lateinit var appScope: AppScope

  private val mediaVM: MediaViewModel by viewModels()
  private val storagePermission by StoragePermissionDelegate

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkLauncherIntent()
    setupExtra()

    Timber.d("MainActivity onCreate ${mediaVM.pendingGranted.size}")

    setContent {
      MaterialDesign3Theme {
        MusicComposeDefault {
          mediaVM.pendingGranted.syncEachClear()
          mediaVM.pendingNewIntent.forEachClear { mediaVM.handleMediaIntent(it) }
        }
      }
    }
  }

  private fun <T> MutableList<T>.forEachClear(lock: Any = this, each: (T) -> Unit) {
    synchronized(lock) {
      forEach { each(it) }
      clear()
    }
  }

  private fun setupExtra() {
    disableFitWindow()
    setCurrentHash(this)
    setCurrentState(this, Alive)
  }

  override fun onStart() {
    Timber.d("MainActivity onStart")
    mediaVM.connectService()
    setCurrentState(this, Visible)
    return super.onStart()
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    Timber.d("MainActivity onPostCreate")
    return super.onPostCreate(savedInstanceState)
  }

  override fun onResume() {
    Timber.d("MainActivity onResume")
    setCurrentState(this, Ready)
    return super.onResume()
  }

  override fun onNewIntent(intent: Intent?) {
    Timber.d("MainActivity onNewIntent $intent, type = ${intent?.type}, scheme = ${intent?.scheme}")
    intent?.let {
      require(IntentValidator.hasKey(it))
      handleIntent(IntentWrapper(intent))
    }
    return run {
      Timber.d("onNewIntent returning with " +
        "\npendingGranted = ${mediaVM.pendingGranted.size}")
      super.onNewIntent(intent)
    }
  }

  override fun onStop() {
    Timber.d("onStop")
    setCurrentState(this, Alive)
    return super.onStop()
  }

  override fun onDestroy() {
    Timber.d("onDestroy")
    setCurrentState(this, Destroyed)
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

  private val storagePermToast by lazy {
    Toast.makeText(this,"Storage Permission Needed", Toast.LENGTH_LONG)
  }

  private fun handleIntent(wrapped: com.kylentt.mediaplayer.helper.external.IntentWrapper) {
    verifyMainThread()
    if (!wrapped.shouldHandleIntent) return
    if (!storagePermission || mediaVM.pendingNewIntent.isNotEmpty()) {
      if (wrapped.isActionView()) {
        mediaVM.pendingNewIntent.removeAll { it.isActionView() }
      }
      mediaVM.pendingNewIntent.add(wrapped)
      return if (!storagePermission) storagePermToast.show() else Unit
    }
    launchHandleIntent(wrapped)
  }

  private fun launchHandleIntent(wrapped: com.kylentt.mediaplayer.helper.external.IntentWrapper) {
    mediaVM.handleMediaIntent(wrapped)
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
      get() = Lifecycle.stateStr

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

      val wasLaunched
        get() = currentActivityState != Nothing
          && currentActivityHash != Nothing
      val isAlive
        get() = currentActivityState >= Alive
      val isVisible
        get() = currentActivityState >= Visible
      val isReady
        get() = currentActivityState >= Ready
      val isDestroyed
        get() = currentActivityState == Destroyed
      val stateStr
        get() = toActivityStateStr(currentActivityState)

      fun toActivityStateStr(@ActivityState int: Int): String {
        return when (int) {
          Nothing -> "Not Launched"
          Destroyed -> "Destroyed"
          Alive -> "Alive"
          Visible -> "Visible"
          Ready -> "Ready"
          else -> "INVALID"
        }
      }

      fun setCurrentHash(activity: Activity) {
        currentActivityHash = activity.hashCode()
        return Timber.d(
          "MainActivity HashCode changed," +
            "\n from: $currentActivityHash" +
            "\n to: ${this.hashCode()}"
        )
      }

      fun setCurrentState(activity: Activity, @ActivityState state: Int) {
        // onDestroy() may be called late, usually after other onPostCreate()
        if (state == Destroyed) if (!currentHashEqual(activity)) return
        require(currentHashEqual(activity))
        currentActivityState = state
      }

      private fun currentHashEqual(activity: Activity): Boolean {
        return activity.hashCode() == currentActivityHash
      }

    }

  }


}
