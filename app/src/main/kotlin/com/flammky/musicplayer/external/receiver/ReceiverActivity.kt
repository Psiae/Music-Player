package com.flammky.musicplayer.external.receiver

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.flammky.android.medialib.temp.common.intent.isActionView
import com.flammky.musicplayer.R
import com.flammky.musicplayer.ui.main.MainActivity
import timber.log.Timber

class ReceiverActivity : Activity() {

	override fun onCreate(savedInstanceState: Bundle?) {
		Log.d("", "")
		Timber.d("")

		super.onCreate(savedInstanceState)
		when {
			intent.isActionView() -> launchMainActivity()
			else -> TODO("Unsupported Intent Action: ${intent.action}")
		}
		finishAfterTransition()
	}

	private fun launchMainActivity() {
		MainActivity.launch(this, intent)
		if (!MainActivity.Companion.Info.state.wasLaunched()) {
			overridePendingTransition(R.anim.anim_stay_still, R.anim.anim_stay_still)
		}
	}
}
