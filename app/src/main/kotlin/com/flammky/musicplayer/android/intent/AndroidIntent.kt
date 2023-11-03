package com.flammky.musicplayer.android.intent

import android.net.Uri

// Immutable kt version of android.content.Intent
class AndroidIntent private constructor(
	val action: String?,
	val uri: Uri?,
	val mimeType: String?
) {

	companion object {

		fun fromIntent(intent: android.content.Intent): AndroidIntent {
			return AndroidIntent(
				action = intent.action,
				uri = intent.data,
				mimeType = intent.type
			)
		}
	}
}

class MutableAndroidIntent(

) {

}

val AndroidIntent.scheme: String?
	get() = uri?.scheme
