package com.flammky.android.medialib.temp.provider.mediastore.base.audio

import com.flammky.android.medialib.temp.provider.mediastore.base.media.MediaEntityProvider

interface AudioEntityProvider<
		E : MediaStoreAudioEntity,
		F : MediaStoreAudioFile,
		M : MediaStoreAudioMetadata,
		Q : MediaStoreAudioQuery
		> : MediaEntityProvider<E, F, M, Q>
