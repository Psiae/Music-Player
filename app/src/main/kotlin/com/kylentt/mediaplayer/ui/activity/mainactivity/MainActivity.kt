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
import androidx.annotation.MainThread
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.kylentt.mediaplayer.app.coroutines.AppDispatchers
import com.kylentt.mediaplayer.app.coroutines.AppScope
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
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.MainActivityContent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  @Inject lateinit var appScope: AppScope
  @Inject lateinit var dispatchers: AppDispatchers

  private val mainViewModel: MainViewModel by viewModels()
  private val mediaViewModel: MediaViewModel by viewModels()
  private val storagePermission: Boolean by StoragePermissionDelegate

  private val storagePermToast: Toast? by lazy {
    Toast.makeText(this, "Storage Permission Needed", Toast.LENGTH_LONG)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    checkLauncherIntent()
    setupActivity()
    setupService()
    setContent {
      MainActivityContent()
    }
  }

  override fun onStart() {
    super.onStart()
    return visible()
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    Timber.d("onNewIntent $intent\nwith Uri: ${intent?.data}")
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
    super.onStop()
    return alive()
  }

  override fun onDestroy() {
    super.onDestroy()
    return destroyed()
  }

  private fun checkLauncherIntent() {
    requireNotNull(intent)
    check(intent.action == Companion.Defaults.intentAction)
    setAsCurrentHash()
    return alive()
  }

  private fun setupActivity() {
    disableWindowFitSystemDecor()
    installSplashScreen()
  }

  private fun setupService() {
    connectMediaService()
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
      // RequireStoragePermission Composable is recomposed OnResume,
      // always after onNewIntent() that might call this function
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

    val stateStr
      @JvmStatic get() = LifecycleState.stateStr
    val wasLaunched
      @JvmStatic get() = LifecycleState.wasLaunched
    val isAlive
      @JvmStatic get() = LifecycleState.isAlive
    val isVisible
      @JvmStatic get() = LifecycleState.isVisible
    val isDestroyed
      @JvmName("isStateDestroyed")
      @JvmStatic get() = LifecycleState.isDestroyed

    @MainThread
    fun startActivity(
      launcher: Context,
      intent: Intent? = null
    ) = with(launcher) {
      verifyMainThread()
      if (!isAlive) {
        // Might be safer to just always Launch this first
        // but I want to see how consistent this is
        startActivity(Defaults.getDefaultIntent(this))
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

      @ActivityState
      private var currentActivityHash = Nothing
      @ActivityState
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

      fun toActivityStateStr(@ActivityState state: Int) =
        when (state) {
          Nothing -> "Not Launched"
          Destroyed -> "Destroyed"
          Alive -> "Alive"
          Visible -> "Visible"
          Ready -> "Ready"
          else -> "INVALID"
        }

      @MainThread
      fun setCurrentHash(activity: Activity) {
        currentActivityHash = activity.hashCode()
        return Timber.d(
          "MainActivity HashCode changed," +
            "\n from: $currentActivityHash" +
            "\n to: ${this.hashCode()}"
        )
      }

      @MainThread
      fun setCurrentState(
        activity: Activity,
        @ActivityState state: Int
      ) {
        if (state == Destroyed) {
          // onDestroy() may be called late, usually after other onPostCreate()
          if (!currentHashEqual(activity)) return
        }
        require(state in (Nothing + 1)..Ready)
        require(currentHashEqual(activity))
        currentActivityState = state
        return Timber.d("MainActivity state changed to $stateStr")
      }

      private fun currentHashEqual(activity: Activity): Boolean {
        return activity.hashCode() == currentActivityHash
      }
    }

    object Defaults {
      const val intentAction = Intent.ACTION_MAIN
      private val defClass = MainActivity::class.java
      private lateinit var defIntent: Intent

      fun appendAction(intent: Intent) =
        intent.apply { action = intentAction }

      fun appendClass(context: Context, intent: Intent) =
        intent.apply { setClass(context, defClass) }

      fun getDefaultIntent(context: Context): Intent {
        if (!::defIntent.isInitialized) {
          defIntent = Intent()
            .appendMainActivityAction()
            .appendMainActivityClass(context)
        }
        return defIntent
      }
    }

  }
}
