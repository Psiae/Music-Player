package com.flammky.android.medialib.providers.mediastore.internal

import com.flammky.android.medialib.providers.mediastore.MediaStoreContext
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.api28.MediaStoreProvider28
import com.flammky.androidx.sdk.VersionHelper

internal class RealMediaStoreProvider(context: MediaStoreContext) : MediaStoreProvider {

	private val impl: MediaStoreProvider = when {
		VersionHelper.hasR() -> /* Not yet implemented */ MediaStoreProvider28(context)
		VersionHelper.hasQ() -> /* Not yet implemented */ MediaStoreProvider28(context)
		else ->  MediaStoreProvider28(context)
	}

	override val audio: MediaStoreProvider.Audio = impl.audio
	override val image: MediaStoreProvider.Image = impl.image
	override val video: MediaStoreProvider.Video = impl.video
}
