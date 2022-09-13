package com.flammky.android.medialib.temp.provider.mediastore.base

import com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider

abstract class MediaStoreProviderBase {
	abstract val audio: MediaStoreProvider.Audio
	abstract fun isGenerationChanged(): Boolean
}
