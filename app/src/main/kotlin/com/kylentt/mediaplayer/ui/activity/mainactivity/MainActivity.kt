package com.kylentt.mediaplayer.ui.activity.mainactivity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.IntDef
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kylentt.mediaplayer.app.AppDispatchers
import com.kylentt.mediaplayer.app.AppScope
import com.kylentt.mediaplayer.app.delegates.AppDelegate
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionDelegate
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.mediaplayer.helper.Preconditions.verifyMainThread
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.ui.activity.ActivityExtension.disableWindowFitSystemDecor
import com.kylentt.mediaplayer.ui.activity.IntentExtension.appendMainActivityAction
import com.kylentt.mediaplayer.ui.activity.IntentExtension.appendMainActivityClass
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity.Companion.LifecycleState.Alive
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity.Companion.LifecycleState.Destroyed
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity.Companion.LifecycleState.Ready
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity.Companion.LifecycleState.Visible
import com.kylentt.mediaplayer.ui.activity.mainactivity.MainActivity.Companion.LifecycleState.toActivityStateStr
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.MainActivityContent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var appScope: AppScope
  @Inject lateinit var dispatcher: AppDispatchers

  private val mainViewModel: MainViewModel by viewModels()
  private val mediaViewModel: MediaViewModel by viewModels()
  private val storagePermission: Boolean by StoragePermissionDelegate

  private val storagePermToast by lazy {
    Toast.makeText(this, "Storage Permission Needed", Toast.LENGTH_LONG)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkLauncherIntent()
    setupActivity()
    setContent {
      MainActivityContent()
    }
  }

  private fun checkLauncherIntent() {
    requireNotNull(intent)
    check(intent.action == Companion.Defaults.intentAction)
    setAsCurrentHash()
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

  private fun setupActivity() {
    disableWindowFitSystemDecor()
    installSplashScreen()
    connectMediaService()
  }

  override fun onBackPressed() {
    super.onBackPressed()
  }

  private fun connectMediaService() = mediaViewModel.connectService()

  private fun handleIntent(intent: IntentWrapper) {
    verifyMainThread()
    if (!intent.shouldHandleIntent) return
    if (!storagePermission) {
      mainViewModel.pendingStorageIntent.add(intent)
      storagePermToast?.show()
      return
    }
    if (mainViewModel.pendingStorageIntent.isNotEmpty()) {
      mainViewModel.pendingStorageIntent.add(intent)
      return
    }
    mediaViewModel.handleMediaIntent(intent)
  }

  private fun setAsCurrentHash() = LifecycleState.setCurrentHash(this)
  private fun alive() = LifecycleState.setCurrentState(this, Alive)
  private fun visible() = LifecycleState.setCurrentState(this, Visible)
  private fun ready() = LifecycleState.setCurrentState(this, Ready)
  private fun destroyed() = LifecycleState.setCurrentState(this, Destroyed)

  companion object {

    val wasLaunched
      get() = LifecycleState.wasLaunched
    val isAlive
      get() = LifecycleState.isAlive
    val isVisible
      get() = LifecycleState.isVisible
    val isDestroyed
      get() = LifecycleState.isDestroyed

    val stateStr
      get() = LifecycleState.stateStr

    fun startActivity(
      launcher: Context,
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

    private object LifecycleState {

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

      fun toActivityStateStr(@ActivityState int: Int) =
        when (int) {
          Nothing -> "Not Launched"
          Destroyed -> "Destroyed"
          Alive -> "Alive"
          Visible -> "Visible"
          Ready -> "Ready"
          else -> "INVALID"
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
        return Timber.d("MainActivity state changed to ${toActivityStateStr(state)}")
      }

      private fun currentHashEqual(activity: Activity): Boolean {
        return activity.hashCode() == currentActivityHash
      }
    }

    object Defaults {
      const val intentAction = Intent.ACTION_MAIN
      private val defClass = MainActivity::class.java

      fun appendAction(intent: Intent) =
        intent.apply { action = intentAction }

      fun appendClass(context: Context, intent: Intent) =
        intent.apply { setClass(context, defClass) }

      fun makeDefaultIntent(context: Context): Intent {
        return Intent()
          .appendMainActivityAction()
          .appendMainActivityClass(context)
      }
    }
  }
}
