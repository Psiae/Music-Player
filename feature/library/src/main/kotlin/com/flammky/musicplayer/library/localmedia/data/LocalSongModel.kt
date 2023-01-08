package com.flammky.musicplayer.library.localmedia.data

import android.net.Uri
import androidx.compose.runtime.Immutable
import com.flammky.android.medialib.common.mediaitem.MediaMetadata

@Immutable
data class LocalSongModel(
	val id: String,
	val displayName: String?,
	val uri: Uri,
	val metadata: MediaMetadata
) {

}
