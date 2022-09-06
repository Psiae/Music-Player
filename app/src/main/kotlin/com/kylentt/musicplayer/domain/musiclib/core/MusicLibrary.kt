package com.kylentt.musicplayer.domain.musiclib.core

import android.content.Context
import com.kylentt.musicplayer.common.lazy.LazyConstructor
import com.kylentt.musicplayer.domain.musiclib.core.internal.ComponentModule
import com.kylentt.musicplayer.domain.musiclib.core.public.MusicLibraryAPI

@Deprecated("use MediaLibrary instead")
class MusicLibrary private constructor(
	private val context: Context
) {

	private val publicAPI: MusicLibraryAPI
	init {
		performConstruct(context)
		publicAPI = MusicLibraryAPI()
	}

	private fun performConstruct(context: Context) {
		ComponentModule.instance.attachContext(context)
	}

	companion object {
		private val constructor = LazyConstructor<MusicLibrary>()
		private val instance by constructor

		val api: MusicLibraryAPI
			get() {
				check(constructor.isConstructed()) {
					"MusicLibrary was not initialized"
				}
				return instance.publicAPI
			}

		fun construct(context: Context): MusicLibraryAPI {
			constructor.construct { MusicLibrary(context) }
			return api
		}
	}
}
