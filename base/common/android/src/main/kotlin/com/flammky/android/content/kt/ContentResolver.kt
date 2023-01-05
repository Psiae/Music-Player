package com.flammky.android.content.kt

import android.content.ContentResolver
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Observe the given [Uri] on the given [ContentResolver]
 * @param uri the uri to observe
 * @param includeChild whether to also collect its descendant's event
 * @param channelBuffer the observer buffer, defaults to CONFLATED,
 * use UNLIMITED if every event matters
 */
fun ContentResolver.observeUri(
	uri: Uri,
	includeChild: Boolean,
	channelBuffer: Int = Channel.BUFFERED
): Flow<ObserveContentEvent> {
	return flow {
		val channel = Channel<ObserveContentEvent>(channelBuffer)

		val contentUriObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
			override fun onChange(selfChange: Boolean) {
				channel.trySend(ObserveContentEvent(selfChange, null, null, null))
			}

			override fun onChange(selfChange: Boolean, uri: Uri?) {
				channel.trySend(ObserveContentEvent(selfChange, uri, null, null))
			}

			override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
				channel.trySend(ObserveContentEvent(selfChange, uri, null, flags))
			}

			override fun onChange(selfChange: Boolean, uris: MutableCollection<Uri>, flags: Int) {
				channel.trySend(ObserveContentEvent(selfChange, null, uris, flags))
			}
		}

		registerContentObserver(uri, includeChild, contentUriObserver)

		try {
			for (event in channel) {
				emit(event)
			}
		} finally {
			unregisterContentObserver(contentUriObserver)
		}
	}
}

/**
 * @param selfChange whether the said change is not caused by external factor
 * **
 * parameters below is unavoidable as the callback is not consistent across API / Devices
 * a Bulk change might be delegated independently (directly not by super call)
 * a Single change might also be delegated with collection
 * a Flag might also not be given (usually happen on certain vendor)
 * the Uri might also not be given ¯\_(ツ)_/¯
 * **
 * @param uri the changed Uri
 * @param uris collection of changed Uri
 * @param flags the flags indicating the reason of the change
 */
data class ObserveContentEvent(
	val selfChange: Boolean,
	val uri: Uri?,
	val uris: MutableCollection<Uri>?,
	val flags: Int?
)
