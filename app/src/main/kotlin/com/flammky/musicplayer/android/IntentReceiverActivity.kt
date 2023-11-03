package com.flammky.musicplayer.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.flammky.android.content.intent.isActionView
import com.flammky.musicplayer.R
import com.flammky.musicplayer.android.base.activity.ActivityWatcher
import com.flammky.musicplayer.main.MainActivity

/**
 * For now use this activity to delegate all incoming intent (except launcher)
 */
class IntentReceiverActivity : Activity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		when {
			intent.isActionView() -> resolveActionView(intent)
			else -> {
				// Display unsupported intent
				TODO("Unsupported Intent: $intent")
			}
		}
		finishAfterTransition()
	}

	private fun resolveActionView(intent: Intent) {
		require(intent.isActionView())
		delegateIntentToMainActivity()
	}

	private fun delegateIntentToMainActivity() {
		MainActivity.launchWithIntent(
			launcherContext = this,
			intent = requireNotNull(intent)
		).also { success ->
			if (success && !ActivityWatcher.get().hasActivity(MainActivity::class.java)) {
				overridePendingTransition(R.anim.anim_stay_still, R.anim.anim_stay_still)
			}
		}
	}
}
