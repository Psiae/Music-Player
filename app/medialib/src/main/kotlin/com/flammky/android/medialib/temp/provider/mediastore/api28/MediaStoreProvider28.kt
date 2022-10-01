package com.flammky.android.medialib.temp.provider.mediastore.api28

import com.flammky.android.medialib.common.mediaitem.MediaItemFactoryOf
import com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.provider.mediastore.MediaStoreContext
import com.flammky.android.medialib.temp.provider.mediastore.api28.audio.AudioEntityProvider28
import com.flammky.android.medialib.temp.provider.mediastore.api28.audio.MediaStoreAudioEntity28
import com.flammky.android.medialib.temp.provider.mediastore.base.MediaStoreProviderBase
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.MediaStoreAudioEntity

internal class MediaStoreProvider28 internal constructor(private val context: MediaStoreContext) : MediaStoreProviderBase() {
	override val audio: MediaStoreProvider.Audio = Audio()

	private inner class Audio : MediaStoreProvider.Audio {
		private val provider = AudioEntityProvider28(context)

		override val mediaItemFactory: MediaItemFactoryOf<MediaStoreAudioEntity>
			get() = provider.mediaItemFactory
		override suspend fun query(): List<MediaStoreAudioEntity28> = provider.queryEntity(true)

		override fun registerOnContentChanged(onContentChangedListener: MediaStoreProvider.OnContentChangedListener) {
			provider.registerOnContentChangedListener(onContentChangedListener)
		}

		override fun unregisterOnContentChanged(onContentChangedListener: MediaStoreProvider.OnContentChangedListener) {
			provider.unregisterOnContentChangedListener(onContentChangedListener)
		}
	}
}
