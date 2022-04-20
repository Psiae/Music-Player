package com.kylentt.mediaplayer.ui.activity.mainactivity

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
import com.kylentt.mediaplayer.app.AppDispatchers
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.ui.activity.ActivityExtension.disableWindowFitSystemDecor
import com.kylentt.mediaplayer.ui.activity.IntentExtension.appendMainActivityAction
import com.kylentt.mediaplayer.ui.activity.IntentExtension.appendMainActivityClass
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity.Companion.Lifecycle.setCurrentHash
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity.Companion.Lifecycle.setCurrentState
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.MainActivityContent
import timber.log.Timber
import javax.inject.Inject

interface ExposeActivityState {
  fun setCurrent()
  fun alive()
  fun visible()
  fun ready()
  fun destroyed()
}

internal class MainActivity : ComponentActivity(), ExposeActivityState {

  @Inject lateinit var appScope: AppScope
  @Inject lateinit var dispatcher: AppDispatchers

  private val mainViewModel: MainViewModel by viewModels()
  private val mediaViewModel: MediaViewModel by viewModels()
  private val storagePermission: Boolean by StoragePermissionDelegate

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkLauncherIntent()
    installSplashScreen()
    setupExtra()
    setContent {
      MainActivityContent(mainViewModel)
    }
  }

  private fun checkLauncherIntent() {
    requireNotNull(intent)
    check(intent.action == Defaults.intentAction)
    setCurrent()
    return alive()
  }

  override fun onStart() {
    super.onStart()
    return visible()
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    intent?.let { handleIntent(IntentWrapper.fromIntent(it)) }
  }

  override fun onResume() {
    super.onResume()
    return ready()
  }

  override fun onPause() {
    super.onPause()
    return visible()
  }

  override fun onStop() {
    return run {
      alive()
      super.onStop()
    }
  }

  override fun onDestroy() {
    return run {
      destroyed()
      super.onDestroy()
    }
  }

  override fun setCurrent() = setCurrentHash()
  override fun alive() = setCurrentState(Lifecycle.Alive)
  override fun visible() = setCurrentState(Lifecycle.Visible)
  override fun ready() = setCurrentState(Lifecycle.Ready)
  override fun destroyed() = setCurrentState(Lifecycle.Destroyed)

  private val storagePermToast = Toast.makeText(
    this, "Storage Permission Needed", Toast.LENGTH_LONG
  )

  private fun setupExtra() {
    connectMediaService()
    disableWindowFitSystemDecor()
  }

  private fun connectMediaService() = mediaViewModel.connectService()

  private fun handleIntent(intent: IntentWrapper) {
    verifyMainThread()
    if (!intent.shouldHandleIntent) {
      return
    }
    if (!storagePermission) {
      mainViewModel.pendingStorageGranted.add { handleIntent(intent) }
      storagePermToast.show()
      return
    }
    mediaViewModel.handleMediaIntent(intent)
  }

  companion object {
    val wasLaunched
      get() = Lifecycle.wasLaunched
    val isAlive
      get() = Lifecycle.isAlive
    val isVisible
      get() = Lifecycle.isVisible
    val isDestroyed
      get() = Lifecycle.isDestroyed

    fun startActivity(
      launcher: Activity,
      intent: Intent? = null
    ) = with(launcher) {
      if (!isAlive) {
        // Might be safer to just always Launch this first
        // but I want to see how consistent this is
        startActivity(Defaults.makeDefaultIntent(this))
      }
      if (intent != null) {
        intent.appendMainActivityClass(this)
        startActivity(intent)
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

      fun toActivityStateStr(@ActivityState int: Int) =
        when (currentActivityState) {
          Nothing -> "Not Launched"
          Destroyed -> "Destroyed"
          Alive -> "Alive"
          Visible -> "Visible"
          Ready -> "Ready"
          else -> "INVALID"
        }

      fun MainActivity.setCurrentHash() {
        currentActivityHash = this.hashCode()
        return Timber.d(
          "MainActivity HashCode changed," +
            "\n from: $currentActivityHash" +
            "\n to: ${this.hashCode()}"
        )
      }

      fun MainActivity.setCurrentState(@ActivityState state: Int) {
        // onDestroy() may be called late, usually after other onPostCreate()
        if (state == Destroyed) if (!hashEqual()) return
        require(hashEqual())
        currentActivityState = state
      }

      private fun MainActivity.hashEqual(): Boolean {
        return this.hashCode() == currentActivityHash
      }

    }

    object Defaults {
      const val intentAction = Intent.ACTION_MAIN
      private val defClass = MainActivity::class.java

      fun appendClass(context: Context, intent: Intent) = intent
        .apply { setClass(context, defClass) }

      fun appendAction(intent: Intent) = intent
        .apply { action = intentAction }

      fun makeDefaultIntent(context: Context): Intent {
        return Intent()
          .appendMainActivityAction()
          .appendMainActivityClass(context)
      }

    }

  }

}
