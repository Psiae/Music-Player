package com.kylentt.mediaplayer.ui.activity.mainactivity

import android.app.Activity
import android.app.Service
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
import com.kylentt.mediaplayer.core.coroutines.CoroutineDispatchers
import com.kylentt.mediaplayer.core.coroutines.AppScope
import com.kylentt.mediaplayer.core.delegates.LateLazy
import com.kylentt.mediaplayer.core.delegates.LockMainThread
import com.kylentt.mediaplayer.app.delegates.device.StoragePermissionHelper
import com.kylentt.mediaplayer.domain.musiclib.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.mediaplayer.helper.Preconditions.checkArgument
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.ui.activity.ActivityExtension.disableWindowFitSystem
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
  @Inject lateinit var dispatchers: CoroutineDispatchers

  private val mainViewModel: MainViewModel by viewModels()
  private val mediaViewModel: MediaViewModel by viewModels()
  private val storagePermission: Boolean by StoragePermissionHelper
	private val serviceState by MusicLibraryService.StateDelegate

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
    Timber.d("onNewIntent, intent: $intent")
    intent?.let {
      val wrapper = IntentWrapper.fromIntent(it)
      Timber.d("onNewIntent, wrapper: $wrapper")
      handleIntent(wrapper)
    }
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
    checkNotNull(intent) {
      "Intent must not be Null."
    }
    checkState(intent.action == Defaults.intentAction) {
      "MainActivity Intent.action must Always be ${Defaults.intentAction}, caught: $intent"
    }
    setAsCurrentHash()
    return alive()
  }

  private fun setupActivity() {
    disableWindowFitSystem()
    installSplashScreen()
  }

  private fun setupService() {
    connectMediaService()
  }

  private fun connectMediaService() = mediaViewModel.connectService()

  private fun handleIntent(intent: IntentWrapper) {
    checkMainThread()
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
  private fun alive() = LifecycleState.updateState(this, Alive)
  private fun visible() = LifecycleState.updateState(this, Visible)
  private fun ready() = LifecycleState.updateState(this, Ready)
  private fun destroyed() = LifecycleState.updateState(this, Destroyed)

  companion object {

    /**
     * for Debugging purposes
     *
     * [String] representation of current [LifecycleState.currentActivityState]
     * @see LifecycleState.ActivityState
     */

    @JvmStatic
    val stateString
      get() = LifecycleState.stateString

    /**
     * check if [MainActivity] has been Launched
     *
     * [Boolean] true if [LifecycleState.currentActivityState] > [LifecycleState.Nothing]
     * @see LifecycleState.ActivityState
     */

    @JvmStatic
    val wasLaunched
      get() = LifecycleState.wasLaunched

    /**
     * check if [MainActivity] is at least [Alive]
     *
     * [Boolean] true if [LifecycleState.currentActivityState] >= [LifecycleState.Alive]
     * @see LifecycleState.ActivityState
     */

    @JvmStatic
    val isAlive
       get() = LifecycleState.isAlive

    /**
     * check if [MainActivity] is at least [Visible] to the User
     *
     * [Boolean] true if [LifecycleState.currentActivityState] >= [LifecycleState.Visible]
     * @see LifecycleState.ActivityState
     */

    @JvmStatic
    val isVisible
       get() = LifecycleState.isVisible

    /**
     * check if [MainActivity] is [Ready] for User Interaction
     *
     * [Boolean] true if [LifecycleState.currentActivityState] == [LifecycleState.Ready]
     * @see LifecycleState.ActivityState
     */

    @JvmStatic
    val isReady
      get() = LifecycleState.isReady

    /**
     * check if [MainActivity] is [Destroyed] or ![isAlive] but [wasLaunched]
     *
     * [Boolean] true if [LifecycleState.currentActivityState] == [LifecycleState.Destroyed]
     * @see LifecycleState.ActivityState
     */

    @JvmStatic
    val isDestroyed
      @JvmName("isStateDestroyed")
       get() = LifecycleState.isDestroyed

    /**
     * Static Method to Launch [MainActivity] with its Default Intent.
     *
     * Starting after targeting Android 12, [Service] must not start any [Activity]
     *
     * @param launcher [Context] to Launch [MainActivity]
     * @param intent [Intent] to send after [MainActivity] is Launched
     * @throws IllegalStateException Must be called from [MainThread]
     * @throws IllegalArgumentException [launcher] Must not be [Service]
     */

    @MainThread
    @JvmStatic
    fun startActivity(
      launcher: Context,
      intent: Intent? = null
    ) {
      checkMainThread()
      checkArgument(launcher !is Service) {
        "Service Cannot Start MainActivity"
      }

      val defIntent = Defaults.getDefaultIntent(launcher)
      launcher.startActivity(defIntent)

      if (intent != null) {
        val clone = intent.clone() as Intent
        val appended = clone.appendMainActivityClass(launcher)
        launcher.startActivity(appended)
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

      private var currentActivityHash by LockMainThread(Nothing)
      private var currentActivityState by LockMainThread(Nothing)

      val wasLaunched
        get() = currentActivityState > Nothing
          && currentActivityHash != Nothing
      val isAlive
        get() = currentActivityState >= Alive
      val isVisible
        get() = currentActivityState >= Visible
      val isReady
        get() = currentActivityState == Ready
      val isDestroyed
        get() = currentActivityState == Destroyed
      val stateString
        get() = toActivityStateStr(currentActivityState)

      /**
       * For Debugging Purposes
       * @param state [Int] Representation of [ActivityState]
       * @return [String] Representation of [ActivityState]
       * @throws IllegalArgumentException [state] Must be [ActivityState]
       * @throws IllegalStateException Should never Throw This
       */

      fun toActivityStateStr(@ActivityState state: Int): String {
        checkArgument(state in Nothing..Ready) {
          "Invalid ActivityState"
        }
        return when (state) {
          Nothing -> "Not Launched"
          Destroyed -> "Destroyed"
          Alive -> "Alive"
          Visible -> "Visible"
          Ready -> "Ready"
          else -> throw IllegalStateException()
        }
      }

      /**
       * Change [LifecycleState.currentActivityHash] for Validation
       * @param activity the [Activity] to get the HashCode
       * @throws IllegalStateException Must be called from [MainThread]
       */

      @MainThread
      fun setCurrentHash(activity: Activity) {
        checkMainThread()
        val hash = activity.hashCode()
        val currentHash = currentActivityHash
        val msg = { "MainActivity HashCode changed,\n from: $currentHash\n to: $currentActivityHash" }
        if (hash == currentHash) {
          return Timber.e("setCurrentHash was called twice $hash")
        }
        currentActivityHash = hash
        Timber.d(msg())
      }

      /**
       * Change [LifecycleState.currentActivityState]
       * @param activity the [Activity] to check the HashCode
       * @param state the [ActivityState]
       * @throws IllegalStateException Must be called from [MainThread]
       * @throws IllegalStateException [activity] HashCode must equal to [currentActivityHash]
       * @throws IllegalArgumentException [state] Must be [ActivityState] and Not [Nothing]
       */

      @MainThread
      fun updateState(
        activity: Activity,
        @ActivityState state: Int
      ) {
        checkMainThread()
        // onDestroy might be called Late
        if (state == Destroyed) if (!currentHashEqual(activity)) return
        checkState(currentHashEqual(activity)) {
          "Not Destroyed ${activity.hashCode()} tried to update $currentActivityHash State"
        }
        checkArgument(state in (Nothing + 1)..Ready) {
          if (state == Nothing) {
            "ActivityState should not be re-set"
          } else {
            "$state is Invalid"
          }
        }
        currentActivityState = state
        Timber.d("MainActivity state changed to $stateString")
      }

      /**
       * check if remembered HashCode is Equal to @param [activity] HashCode
       * @param [activity] the Activity to check the HashCode
       * @return [Boolean] true if the HashCode is Equal
       */

      private fun currentHashEqual(activity: Activity): Boolean {
        return activity.hashCode() == currentActivityHash
      }
    }

    object Defaults {
      const val intentAction = Intent.ACTION_MAIN

      private val defClass = MainActivity::class.java
      private val defIntentInitializer = LateLazy<Intent>()
      private val defIntent by defIntentInitializer

      /**
       * set @param [intent] action to [Defaults.intentAction]
       * @param intent The [Intent]
       * @return [Intent] with [Defaults.intentAction] set
       */

      fun appendAction(intent: Intent) =
        intent.apply { action = intentAction }

      /**
       * set @param [intent] class to [Defaults.defClass]
       * @param context The [Context] to get the package
       * @param intent The [Intent]
       * @return [Intent] with [Defaults.defClass] set
       */

      fun appendClass(context: Context, intent: Intent) =
        intent.apply { setClass(context, defClass) }

      /**
       * get The Default Launcher Intent for [MainActivity]
       * @param context The [Context] to get the package
       * @return [Intent] the Default Launcher Intent
       */

      fun getDefaultIntent(context: Context): Intent {
        if (!defIntentInitializer.isInitialized) {
          val intent = Intent()
            .appendMainActivityAction()
            .appendMainActivityClass(context)
          defIntentInitializer.initializeValue { intent }
        }
        return defIntent
      }
    }

  }

	object Constants {
		const val LAUNCH_REQUEST_CODE = 444
	}
}
