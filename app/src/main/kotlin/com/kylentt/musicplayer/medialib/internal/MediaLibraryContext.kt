package com.kylentt.musicplayer.medialib.internal

import android.content.Context
import com.kylentt.musicplayer.medialib.internal.android.AndroidContext

class MediaLibraryContext private constructor(context: Context) {

	val android: AndroidContext = AndroidContext(context)

	internal class Builder(private val context: Context) {
		fun build(): MediaLibraryContext = MediaLibraryContext(context)
	}
}


