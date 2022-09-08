package com.kylentt.musicplayer.medialib.android.provider.mediastore.base.audio

import com.kylentt.musicplayer.medialib.android.provider.mediastore.base.media.MediaEntityProvider

interface AudioEntityProvider<E : MediaStoreAudioEntity, F : MediaStoreAudioFile, M : MediaStoreAudioMetadata, Q : MediaStoreAudioQuery>
	: MediaEntityProvider<E, F, M, Q>
