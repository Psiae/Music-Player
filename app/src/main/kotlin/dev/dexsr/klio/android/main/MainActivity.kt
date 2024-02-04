package dev.dexsr.klio.android.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.flammky.android.activity.disableSystemWindowInsets
import com.flammky.android.content.intent.isActionMain
import com.flammky.musicplayer.android.activity.ActivityCompanion
import com.flammky.musicplayer.android.activity.RequireLauncher
import com.flammky.musicplayer.android.main.IntentManager
import dev.dexsr.klio.core.sdk.AndroidAPI
import com.flammky.musicplayer.main.ui.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import dev.dexsr.klio.android.main.root.compose.setComposeRootContent
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : ComponentActivity(), RequireLauncher by Companion {

	private lateinit var _intentManager: IntentManager

	internal val intentManager
		get() = _intentManager

	private val oldImpl = OldImpl()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		oldImpl.onCreate(savedInstanceState)
		setComposeRootContent(fitSystemWindow = false)
	}

	override fun onNewIntent(intent: Intent?) {
		super.onNewIntent(intent)
		oldImpl.onNewIntent(intent)
	}

	private inner class OldImpl() {

		@Deprecated("Remove ViewModel usage entirely")
		private val mainVM: MainViewModel by viewModels()

		fun onCreate(savedInstanceState: Bundle?) {
			_intentManager = IntentManager(mainVM.intentHandler)
			checkLauncherIntent()
			setupWindow()
			setupSplashScreen()

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

		fun onNewIntent(intent: Intent?) {
			if (intent == null) {
				return
			}
			if (intent.isActionMain()) {
				return
			}
			check(intent.getStringExtra(LauncherSignatureKey) == LauncherSignatureCode) {
				"New Intent must have Launcher Signature, possible uncaught condition"
			}
			intentManager.new(intent)
		}

		/**
		 * Check the validity of the launcher intent of this Activity
		 */
		private fun checkLauncherIntent() = check(
			value = intent.isActionMain(),
			lazyMessage = {
				"""
				the `first` intent that launched this activity must be kept `Intent.Action_Main`
			"""
			}
		)

		private fun setupWindow() {
			disableSystemWindowInsets()
		}

		private fun setupSplashScreen() {
			mainVM.splashHolders
				.apply {
					if (contains(EntrySplashHolder)) {
						return@apply
					}
				}
			installSplashScreen().setKeepOnScreenCondition {
				run {
					mainVM.splashHolders.isNotEmpty()
				}.also { keep ->
					Timber.d("MainActivity_DEBUG_onPreDraw: SplashCheckKeepOnScreenCondition=$keep")
				}
			}
		}
	}

	companion object : ActivityCompanion(), RequireLauncher {
		private val EntrySplashHolder = Any()

		private val LauncherSignatureCode = run {
			val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
			(1..16)
				.map { chars.elementAt(Random.nextInt(until = chars.size)) }
				.joinToString(separator = "")
		}
		private val LauncherSignatureKey = "dev.dexsr.klio.android.activity_lsign"

		override fun launchWithIntent(
			launcherContext: Context,
			intent: Intent
		): Boolean {

			val androidAPI = AndroidAPI.buildcode.CODE_INT

			return when {
				androidAPI < 0 -> false
				androidAPI in 1..28 -> launchWithIntentImpl28(launcherContext, intent, true)
				androidAPI < 1000 -> launchWithIntentImpl29(launcherContext, intent, androidAPI <= 33)
				else -> false
			}
		}

		private fun launchWithIntentImpl28(
			launcherContext: Context,
			intent: Intent,
			targeted: Boolean
		): Boolean {

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

		private fun launchWithIntentImpl29(
			launcherContext: Context,
			intent: Intent,
			targeted: Boolean
		): Boolean {

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
