package com.flammky.musicplayer.android.main

import android.content.Intent
import com.flammky.musicplayer.android.intent.AndroidIntent

class IntentIdentifier {

	fun isLocalPlaybackRequestIntent(
		intent: AndroidIntent
	): Boolean {

		intent.action
			?.let { action ->
				if (action != Intent.ACTION_VIEW) {
					return false
				}
			}
			?: return false

		intent.mimeType
			?.let { mime ->
				if (!mime.startsWith("audio/")) {
					return false
				}
			}
			?: return false

		return true
	}
}
