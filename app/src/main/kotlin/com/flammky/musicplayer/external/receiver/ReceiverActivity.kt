package com.flammky.musicplayer.external.receiver

import android.app.Activity
import android.os.Bundle
import com.flammky.musicplayer.R
import com.flammky.android.medialib.temp.common.intent.isActionView
import com.flammky.musicplayer.ui.main.MainActivity

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
		MainActivity.Companion.Launcher.launch(this, intent)
		if (!MainActivity.Companion.Info.state.wasLaunched()) {
			overridePendingTransition(R.anim.anim_stay_still, R.anim.anim_stay_still)
		}
	}
}
