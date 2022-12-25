package com.flammky.musicplayer.main

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.flammky.android.activity.disableWindowFitSystemInsets
import com.flammky.android.content.context.ContextHelper
import com.flammky.android.content.intent.isActionMain
import com.flammky.musicplayer.activity.ActivityCompanion
import com.flammky.musicplayer.activity.RequireLauncher
import com.flammky.musicplayer.main.ui.MainViewModel
import com.flammky.musicplayer.main.ui.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private val contextHelper = ContextHelper(this)
	private val innerIntentHandler = InnerIntentHandler()
	private val mainVM: MainViewModel by viewModels()

	init {
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		checkLauncherIntent()
		setupWindow()
		setupSplashScreen()
		setContent()
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		if (intent == null) {
			return
		}
		if (intent.isActionMain()) {
			return
		}
		check(intent.getStringExtra(LauncherSignatureKey) == LauncherSignatureCode) {
			"New Intent must have Launcher Signature, possible uncaught condition"
		}
		innerIntentHandler.handleIntent(intent)
	}

	private fun checkLauncherIntent() = check(
		value = intent.isActionMain(),
		lazyMessage = {
			"""
				the `first` intent that launched this activity must be kept `Intent.Action_Main`
				otherwise we must keep track of received intent by `action + id` which might be implemented
				in the future but is not priority as of now
			"""
		}
	)

	private fun setupWindow() {
		disableWindowFitSystemInsets()
	}

	private fun setupSplashScreen() {
		installSplashScreen()
	}

	private inner class InnerIntentHandler {
		fun handleIntent(intent: Intent) {
			mainVM.entryCheckWaiter.add { mainVM.intentHandler.handleIntent(intent) }
		}
	}

	companion object : ActivityCompanion(), RequireLauncher {

		private val LauncherSignatureCode = (Random.nextInt() shl 13).toString()
		private val LauncherSignatureKey = "l_sign"

		override fun launchWithIntent(
			launcherContext: Context,
			intent: Intent
		): Boolean {

			if (launcherContext is Service) {
				return false
			}

			//
			// Intent Checker here
			//

			val guardIntent = Intent()
				.apply {
					action = Intent.ACTION_MAIN
					setClass(launcherContext, MainActivity::class.java)
					putExtra(LauncherSignatureKey, LauncherSignatureCode)
				}

			val copyIntent = requireNotNull(
				value = (intent.clone() as? Intent)?.takeIf { it !== intent },
				lazyMessage = { "unable to clone Intent=$intent" }
			).apply {
				setClass(launcherContext, MainActivity::class.java)
				putExtra(LauncherSignatureKey, LauncherSignatureCode)
			}

			launcherContext.startActivity(guardIntent)
			launcherContext.startActivity(copyIntent)
			return true
		}
	}
}
