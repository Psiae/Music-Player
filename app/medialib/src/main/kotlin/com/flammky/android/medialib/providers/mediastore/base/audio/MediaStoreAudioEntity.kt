package com.flammky.android.medialib.providers.mediastore.base.audio

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.base.media.MediaStoreEntity
import javax.annotation.concurrent.Immutable

@Immutable
abstract class MediaStoreAudioEntity internal constructor(
	uid: String,
	uri: Uri,
	override val file: MediaStoreAudioFile,
	override val metadata: MediaStoreAudioMetadata,
	internal override val queryInfo: MediaStoreAudioQuery,
) : MediaStoreEntity(uid, uri, file, metadata, queryInfo)
