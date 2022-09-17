package com.flammky.android.medialib.temp.api.provider.mediastore

import android.content.ContentResolver
import android.net.Uri
import com.flammky.android.medialib.temp.media3.contract.MediaItemFactoryOf
import com.flammky.android.medialib.temp.provider.mediastore.base.audio.*

interface MediaStoreProvider {
	val audio: Audio


	interface Audio {
		val mediaItemFactory: MediaItemFactoryOf<MediaStoreAudioEntity>
		suspend fun query(): List<MediaStoreAudioEntity>

		fun registerOnContentChanged(onContentChangedListener: OnContentChangedListener)
		fun unregisterOnContentChanged(onContentChangedListener: OnContentChangedListener)
	}

	fun interface OnContentChangedListener {

		/** @see [ContentResolver.NOTIFY_FLAGS]*/
		sealed interface Flags {
			/** @see [ContentResolver.NOTIFY_INSERT]*/
			object INSERT : Flags
			/** @see [ContentResolver.NOTIFY_UPDATE]*/
			object UPDATE : Flags
			/** @see [ContentResolver.NOTIFY_DELETE]*/
			object DELETE : Flags

			/** @see [ContentResolver.NOTIFY_SYNC_TO_NETWORK] */
			object SYNC_NETWORK : Flags

			/** @see [ContentResolver.NOTIFY_SKIP_NOTIFY_FOR_DESCENDANTS] */
			object SKIP_NOTIFY : Flags
		}

		fun onContentChange(uris: Collection<Uri>, flag: Flags)
	}
}
