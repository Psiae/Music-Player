package com.kylentt.musicplayer.domain.musiclib.source.mediastore

import android.content.ContentResolver
import android.net.Uri

object MediaStoreInfo {
	const val contentScheme = ContentResolver.SCHEME_CONTENT
	const val fileScheme = ContentResolver.SCHEME_FILE

	object Audio {
		val EXTERNAL_CONTENT_URI: Uri
			get() = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ?: Uri.EMPTY
	}
}
