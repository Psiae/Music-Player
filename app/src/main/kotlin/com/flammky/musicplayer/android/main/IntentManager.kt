package com.flammky.musicplayer.android.main

import android.content.Intent
import android.widget.Toast
import com.flammky.musicplayer.KlioApp
import com.flammky.musicplayer.android.intent.AndroidIntent
import com.flammky.musicplayer.main.ext.IntentReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
	) {coroutineScope.launch(Dispatchers.Main) {
			if (!capability.checkDescription(intent)) {
				// TODO: notify that we are not capable of handling said content, with descriptive message
				Toast.makeText(KlioApp.require(), "Unsupported Media Type Request: ${intent.mimeType}", Toast.LENGTH_LONG).show()
				return@launch
			}
			intentReceiver.sendIntent(
				Intent()
					.apply { action = intent.action ; setDataAndType(intent.uri, intent.mimeType) }
			)
		}
	}
}
