package com.kylentt.musicplayer.ui.main

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.kylentt.mediaplayer.domain.viewmodels.MainViewModel
import com.kylentt.mediaplayer.domain.viewmodels.MediaViewModel
import com.kylentt.mediaplayer.helper.external.IntentWrapper
import com.kylentt.mediaplayer.ui.activity.mainactivity.compose.MainActivityContent
import com.kylentt.musicplayer.common.coroutines.CoroutineDispatchers
import com.kylentt.musicplayer.common.intent.isActionMain
import com.kylentt.musicplayer.core.app.delegates.device.StoragePermissionHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	@Inject lateinit var coroutineDispatchers: CoroutineDispatchers

	private val intentHandler = IntentHandler()

	private val mainVM: MainViewModel by viewModels()
	private val mediaVM: MediaViewModel by viewModels()

	private val storagePermission by StoragePermissionHelper

	override fun onCreate(savedInstanceState: Bundle?) {
		checkLauncherIntent()
		setupWindow()
		super.onCreate(savedInstanceState)
		setupSplashScreen()

		// later
		setContent { MainActivityContent() }

		return updateStateDelegate()
	}

	override fun onStart() {
		super.onStart()

		return updateStateDelegate()
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		intent?.let { intentHandler.handleIntent(it) }
	}

	override fun onResume() {
		super.onResume()

		return updateStateDelegate()
	}

	override fun onPause() {
		super.onPause()

		return updateStateDelegate()
	}

	override fun onStop() {
		super.onStop()

		return updateStateDelegate()
	}

	override fun onDestroy() {
		super.onDestroy()

		return updateStateDelegate()
	}

	private fun checkLauncherIntent() = check(intent.isActionMain())

	private fun setupWindow() {
		requireNotNull(window) {
			"window was null" +
				"call this function when or after Activity.onCreate(Bundle?) is called"
		}

		WindowCompat.setDecorFitsSystemWindows(window, false)
	}

	private fun setupSplashScreen() {
		installSplashScreen()
	}


	private fun updateStateDelegate() = StateDelegate.updateState(this)


	private inner class IntentHandler() {
		fun handleIntent(intent: Intent) {
			val wrapped = IntentWrapper.fromIntent(intent)
			if (!wrapped.shouldHandleIntent) return
			if (!storagePermission) {
				mainVM.pendingStorageIntent.add(wrapped)
				return
			}
			if (mainVM.pendingStorageIntent.isNotEmpty()) {
				// RequireStoragePermission Composable is recomposed OnResume,
				// always after onNewIntent() that might call this function
				mainVM.pendingStorageIntent.add(wrapped)
				return
			}
			mediaVM.handleMediaIntent(wrapped)
		}
	}

	object Launcher {
		fun startActivity(
			context: Context,
			intent: Intent? = null
		) {
			require(context !is Service) {
				"Service cannot start MainActivity"
			}

			val state by StateDelegate

			if (!state.isVisible()) {
				val launchIntent = Intent(context, MainActivity::class.java)
				context.startActivity(launchIntent)
			}

			(intent?.clone() as? Intent)?.let { clone ->
				clone.setClass(context, MainActivity::class.java)
				context.startActivity(clone)
			}
		}
	}

	object StateDelegate : ReadOnlyProperty<Any?, DelegatedState.State> {
		private val stateDelegate = DelegatedState()

		fun updateState(activity: MainActivity) {
			stateDelegate.updateState(activity)
		}

		fun getValue(): DelegatedState.State = stateDelegate.getValue()
		override fun getValue(thisRef: Any?, property: KProperty<*>): DelegatedState.State = getValue()
	}
}
