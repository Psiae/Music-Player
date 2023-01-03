package com.flammky.android.medialib.providers.mediastore.base.audio

import android.net.Uri
import com.flammky.android.medialib.providers.mediastore.base.media.MediaStoreEntity
import javax.annotation.concurrent.Immutable

@Immutable
abstract class MediaStoreAudioEntity internal constructor(
    override val uid: String,
    override val uri: Uri,
    override val file: MediaStoreAudioFile,
    override val metadata: MediaStoreAudioMetadataEntryEntry,
    internal override val queryInfo: MediaStoreAudioQuery,
) : MediaStoreEntity(uid, uri, file, metadata, queryInfo)
