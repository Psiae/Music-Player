package com.flammky.musicplayer.main

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.flammky.android.activity.disableWindowFitSystemInsets
import com.flammky.android.content.intent.isActionMain
import com.flammky.musicplayer.activity.ActivityCompanion
import com.flammky.musicplayer.activity.RequireLauncher
import com.flammky.musicplayer.main.ui.MainViewModel
import com.flammky.musicplayer.root.setRootContent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

	private val mainVM: MainViewModel by viewModels()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		checkLauncherIntent()
		setupWindow()
		setupSplashScreen()
		setRootContent()

		lifecycleScope.launch {
			// consume
			for (message in mainVM.intentRequestErrorMessageChannel) {
				Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
			}
		}

		lifecycleScope.launch {
			// consume
			for (message in mainVM.playbackErrorMessageChannel) {
				Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
			}
		}
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
		mainVM.intentEntryGuardWaiter.add { mainVM.intentHandler.sendIntent(intent) }
	}

	/**
	 * Check the validity of the launcher intent of this Activity
	 */
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
		mainVM.splashHolders
			.apply {
				if (contains(EntrySplashHolder)) {
					return@apply
				}
				add(EntrySplashHolder)
				mainVM.firstEntryGuardWaiter.add { mainVM.splashHolders.remove(EntrySplashHolder) }
			}
		installSplashScreen().setKeepOnScreenCondition {
			run {
				mainVM.splashHolders.isNotEmpty()
			}.also { keep ->
				Timber.d("MainActivity_DEBUG_onPreDraw: SplashCheckKeepOnScreenCondition=$keep")
			}
		}
	}

	companion object : ActivityCompanion(), RequireLauncher {
		private val EntrySplashHolder = Any()

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
