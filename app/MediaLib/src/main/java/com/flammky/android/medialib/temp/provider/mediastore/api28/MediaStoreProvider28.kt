package com.flammky.android.medialib.temp.provider.mediastore.api28

import com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.media3.contract.MediaItemFactoryOf
import com.flammky.android.medialib.temp.provider.mediastore.MediaStoreContext
import com.flammky.android.medialib.temp.provider.mediastore.api28.audio.AudioEntityProvider28
import com.flammky.android.medialib.temp.provider.mediastore.api28.audio.MediaStoreAudioEntity28
import com.flammky.android.medialib.temp.provider.mediastore.base.MediaStoreProviderBase
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.*

internal class MediaStoreProvider28 internal constructor(private val context: MediaStoreContext) : MediaStoreProviderBase() {
	private var rememberedVersion: String = ""

	override val audio: MediaStoreProvider.Audio = Audio()

	override fun isGenerationChanged(): Boolean {
		val v = MediaStore28.getVersion(context.androidContext)
		val equal = v == rememberedVersion
		rememberedVersion = v
		return !equal
	}

	init {
		isGenerationChanged()
	}

	private inner class Audio : MediaStoreProvider.Audio {
		private val provider = AudioEntityProvider28(context)

		override suspend fun query(): List<MediaStoreAudioEntity28> = provider.queryEntity()

		override val mediaItemFactory: MediaItemFactoryOf<MediaStoreAudioEntity>
			get() = provider.mediaItemFactory
	}
}
