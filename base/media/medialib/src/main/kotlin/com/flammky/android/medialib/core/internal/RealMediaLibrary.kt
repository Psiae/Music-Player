package com.flammky.android.medialib.core.internal

import com.flammky.android.medialib.context.internal.RealLibraryContext
import com.flammky.android.medialib.core.MediaLibrary
import com.flammky.android.medialib.providers.MediaProviders
import com.flammky.android.medialib.providers.ProvidersContext
import com.flammky.android.medialib.providers.internal.RealMediaProviders


internal class RealMediaLibrary(context: RealLibraryContext) : MediaLibrary(context) {

	override val mediaProviders: MediaProviders = RealMediaProviders(ProvidersContext(context))
}
