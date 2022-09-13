package com.flammky.android.medialib.temp.api.provider.mediastore.internal

import com.flammky.android.core.sdk.VersionHelper
import com.flammky.android.medialib.temp.api.provider.mediastore.MediaStoreProvider
import com.flammky.android.medialib.temp.provider.mediastore.MediaStoreContext
import com.flammky.android.medialib.temp.provider.mediastore.api28.MediaStoreProvider28
import com.flammky.android.medialib.temp.provider.mediastore.base.MediaStoreProviderBase

internal class RealMediaStoreProvider (private val context: MediaStoreContext) : MediaStoreProvider {

	private val actual: MediaStoreProviderBase = when {
		VersionHelper.hasR() -> /* TODO */ MediaStoreProvider28(context)
		VersionHelper.hasQ() -> /* TODO */ MediaStoreProvider28(context)
		else -> /* TODO */ MediaStoreProvider28(context)
	}

	@Suppress("UNCHECKED_CAST")
	override val audio = actual.audio

	fun isGenerationChanged(): Boolean = actual.isGenerationChanged()
}
