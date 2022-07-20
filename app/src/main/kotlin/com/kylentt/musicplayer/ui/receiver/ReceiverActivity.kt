package com.kylentt.musicplayer.ui.receiver

import android.app.Activity
import android.os.Bundle
import com.kylentt.musicplayer.R
import com.kylentt.musicplayer.common.intent.isActionView
import com.kylentt.musicplayer.ui.main.MainActivity

class ReceiverActivity : Activity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		when {
			intent.isActionView() -> launchMainActivity()
			else -> throw NotImplementedError()
		}
		finishAfterTransition()
	}

	private fun launchMainActivity() {
		val state by MainActivity.StateDelegate
		MainActivity.Launcher.startActivity(this, intent)
		if (!state.wasLaunched()) {
			overridePendingTransition(R.anim.anim_stay_still, R.anim.anim_stay_still)
		}
	}
}
