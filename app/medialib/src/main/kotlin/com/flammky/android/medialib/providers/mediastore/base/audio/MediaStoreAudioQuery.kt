package com.flammky.android.medialib.providers.mediastore.base.audio

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.base.media.MediaStoreQuery

abstract class MediaStoreAudioQuery internal constructor(
	id: Long,
	uri: Uri,
	val albumId: Long?,
	val artistId: Long?
) : MediaStoreQuery(id, uri)
