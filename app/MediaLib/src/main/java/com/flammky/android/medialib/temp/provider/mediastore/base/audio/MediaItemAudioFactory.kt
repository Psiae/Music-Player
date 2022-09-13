package com.flammky.android.medialib.temp.provider.mediastore.base.audio

import com.flammky.android.medialib.temp.provider.mediastore.MediaStoreContext
import com.flammky.android.medialib.temp.provider.mediastore.base.media.MediaItemFactory

abstract class MediaItemAudioFactory<
  E : MediaStoreAudioEntity,
		F : MediaStoreAudioFile,
		M : MediaStoreAudioMetadata,
		Q : MediaStoreAudioQuery
		> internal constructor(context: MediaStoreContext) : MediaItemFactory<E, F, M, Q>(context) {

		}
