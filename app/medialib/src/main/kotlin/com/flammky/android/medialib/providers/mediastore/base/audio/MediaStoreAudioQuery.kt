package com.flammky.android.medialib.providers.mediastore.base.audio

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.base.media.MediaStoreQuery

abstract class MediaStoreAudioQuery internal constructor(
	override val id: Long,
	override val uri: Uri,
	open val albumId: Long?,
	open val artistId: Long?
) : MediaStoreQuery(id, uri)
