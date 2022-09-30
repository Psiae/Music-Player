package com.flammky.musicplayer.ui.main

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.flammky.android.activity.disableWindowFitSystemInsets
import com.flammky.android.medialib.temp.common.context.ContextInfo
import com.flammky.android.medialib.temp.common.intent.isActionMain
import com.flammky.mediaplayer.domain.viewmodels.MainViewModel
import com.flammky.mediaplayer.domain.viewmodels.MediaViewModel
import com.flammky.mediaplayer.helper.external.IntentWrapper
import com.flammky.musicplayer.ui.main.compose.ComposeContent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlin.properties.ReadOnlyProperty

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private val contextInfo = ContextInfo(this)

	private var mReleased: Boolean = false
		set(value) {
			require(value)
			field = value
		}

	private val intentHandler = IntentHandler()

	private val mainVM: MainViewModel by viewModels()
	private val mediaVM: MediaViewModel by viewModels()

	private val readStoragePermission
		get() = contextInfo.permissionInfo.readExternalStorageAllowed

	init {
		dispatchEvent(MainEvent.Init)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		checkLauncherIntent()
		setupWindow()
		setupSplashScreen()

		com.flammky.android.medialib.temp.MediaLibrary.API.service.startService()

		setContent {
			ComposeContent()
		}

		return dispatchEvent(MainEvent.Create)
	}

	override fun onStart() {
		super.onStart()

		return dispatchEvent(MainEvent.Start)
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		intent?.let { intentHandler.handleIntent(it) }
	}

	override fun onResume() {
		super.onResume()

		return dispatchEvent(MainEvent.Resume)
	}

	override fun onPause() {
		super.onPause()

		return dispatchEvent(MainEvent.Pause)
	}

	override fun onStop() {
		super.onStop()

		return dispatchEvent(MainEvent.Stop)
	}

	private fun onRelease() {
		mReleased = true
		return dispatchEvent(MainEvent.Release)
	}

	override fun onDestroy() {
		if (!mReleased) onRelease()
		super.onDestroy()

		return dispatchEvent(MainEvent.Destroy)
	}

	private fun checkLauncherIntent() = check(intent.isActionMain())

	private fun setupWindow() {
		disableWindowFitSystemInsets()
	}

	private fun setupSplashScreen() {
		installSplashScreen()
	}

	private fun dispatchEvent(event: MainEvent) {
		Delegate.onEvent(this, event)
	}

	private inner class IntentHandler {
		fun handleIntent(intent: Intent) {
			val wrapped = IntentWrapper.fromIntent(intent)
			if (!wrapped.shouldHandleIntent) return
			if (!readStoragePermission) {
				mediaVM.pendingStorageIntent.add(wrapped)
				return
			}
			if (mediaVM.pendingStorageIntent.isNotEmpty()) {
				// RequireStoragePermission Composable is recomposed OnResume,
				// always after onNewIntent() that might call this function
				mediaVM.pendingStorageIntent.add(wrapped)
				return
			}
			mediaVM.handleMediaIntent(wrapped)
		}
	}

	sealed class MainEvent {
		object Init : MainEvent()
		object Create : MainEvent()
		object Start : MainEvent()
		object Resume : MainEvent()
		object Pause : MainEvent()
		object Stop : MainEvent()
		object Release : MainEvent()
		object Destroy : MainEvent()
	}

	sealed class MainState {
		object Nothing : MainState()
		object Initialized : MainState()
		object Created : MainState()
		object Started : MainState()
		object Resumed : MainState()
		object Released : MainState()
		object Destroyed : MainState()

		fun wasLaunched() = this != Nothing
		fun isAlive() = wasLaunched() && !isDestroyed()
		fun isCreated() = this == Created || isVisible()
		fun isVisible() = this == Started || isReady()
		fun isReady() = this == Resumed
		fun isDestroyed() = this == Destroyed
	}

	private object Launcher {

		fun startActivity(
			context: Context,
			intent: Intent? = null
		) {

			Intent(context, MainActivity::class.java)
				.apply { action = Intent.ACTION_MAIN }
				.let { context.startActivity(it) }

			(intent?.clone() as? Intent)?.let { clone ->
				clone.setClass(context, MainActivity::class.java)
				context.startActivity(clone)
			}
		}
	}

	private object Delegate {
		private var mState: MainState = MainState.Nothing
		private var mHashCode: Int? = null

		val state
			get() = mState

		fun onEvent(activity: MainActivity, event: MainEvent) {
			val state = when (event) {
				MainEvent.Init -> MainState.Initialized
				MainEvent.Create, MainEvent.Stop -> MainState.Created
				MainEvent.Start, MainEvent.Pause -> MainState.Started
				MainEvent.Resume -> MainState.Resumed
				MainEvent.Release -> MainState.Released
				MainEvent.Destroy -> MainState.Destroyed
			}
			updateState(activity, state)
		}

		private fun updateState(activity: MainActivity, state: MainState) {

			when (state) {
				MainState.Nothing -> throw IllegalArgumentException()
				MainState.Initialized -> mHashCode = activity.hashCode()
				MainState.Destroyed -> if (!hashEqual(activity)) return
				else -> Unit
			}

			require(hashEqual(activity))
			mState = state

			Timber.d("MainActivity StateDelegate, updated to $mState")
		}

		private fun hashEqual(activity: MainActivity) = mHashCode == activity.hashCode()
	}

	companion object {

		/**
		 *  2022-07-28 Note: Consider Static Object managing known App internal environment similar to
		 *  [com.flammky.musicplayer.common.android.environment.DeviceInfo]
		 */

		/**
		 * Static Object providing public Information about this Activity if provided
		 * by current instance backed by its hashCode and not any of its property whatsoever
		 * to ensure there's no Memory Leak.
		 *
		 * &nbsp;
		 *
		 * Does NOT guarantee reliable Information on some occasions,
		 * consider the returned Info as `Possibility`.
		 *
		 * &nbsp;
		 *
		 * For example: [MainState.Created] might be delivered instead of [MainState.Destroyed]
		 * if the instance does not receive onDestroy() callback but usually in this case
		 * there's no other Context based Components running and static object will be cleared out,
		 *
		 * &nbsp;
		 *
		 * or whatever state left if process death occur when Sticky Service is running.
		 *
		 * &nbsp;
		 *
		 * But we can reduce those possibility by interacting with other known Context based Components
		 * such as [Service].
		 *
		 * &nbsp;
		 *
		 * Should only be used by Object that receive System callbacks
		 */

		object Info  {
			val state: MainState get() = Delegate.state
			val stateDelegate = ReadOnlyProperty<Any?, MainState> { _, _ -> state }
		}

		fun launch(context: Context) =
			Launcher.startActivity(context)
		fun launch(context: Context, intent: Intent) =
			Launcher.startActivity(context, intent)
	}
}
