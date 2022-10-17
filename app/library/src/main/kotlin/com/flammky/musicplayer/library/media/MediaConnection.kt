package com.flammky.musicplayer.library.media

import android.net.Uri

/**
 * Media Connection Interface for this library module
 */
internal interface MediaConnection {
	fun play(id: String, uri: Uri)
}
