package com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.audio

import com.kylentt.musicplayer.medialib.internal.provider.mediastore.MediaStoreContext
import com.kylentt.musicplayer.medialib.internal.provider.mediastore.base.media.MediaItemFactory

internal abstract class MediaItemAudioFactory<E : MediaStoreAudioEntity, F : MediaStoreAudioFile, M : MediaStoreAudioMetadata, Q
: MediaStoreAudioQuery>(context: MediaStoreContext) :
	MediaItemFactory<MediaStoreAudioEntity, MediaStoreAudioFile, MediaStoreAudioMetadata, MediaStoreAudioQuery>(
		context
	)
