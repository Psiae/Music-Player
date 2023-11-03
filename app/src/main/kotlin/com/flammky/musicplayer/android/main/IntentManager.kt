package com.flammky.musicplayer.android.main

import android.content.Intent
import com.flammky.musicplayer.android.intent.AndroidIntent
import com.flammky.musicplayer.main.ext.IntentReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IntentManager constructor(
	// TODO: remove
	private val intentReceiver: IntentReceiver

) {

	private val coroutineScope = CoroutineScope(SupervisorJob())

	private val identifier = IntentIdentifier()
	private val capability = LocalPlaybackRequestIntentCapability()

	fun new(intent: Intent) {
		val androidIntent = AndroidIntent.fromIntent(intent = intent)

		// TODO: make session instead
		if (identifier.isLocalPlaybackRequestIntent(androidIntent)) {
			newLocalPlaybackRequestIntent(androidIntent)
			return
		}
	}

	private fun newLocalPlaybackRequestIntent(
		intent: AndroidIntent
	) {
		coroutineScope.launch {
			if (!capability.checkDescription(intent)) {
				// TODO: notify that we are not capable of handling said content, with descriptive message
				return@launch
			}
			intentReceiver.sendIntent(
				Intent()
					.apply { action = intent.action ; setDataAndType(intent.uri, intent.mimeType) }
			)
		}
	}
}
