package com.flammky.android.medialib.providers.mediastore.api28.audio

import android.content.ContentResolver
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.net.Uri
import com.flammky.android.content.context.ContextHelper
import com.flammky.android.io.exception.ReadExternalStoragePermissionException
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreContext
import com.flammky.android.medialib.providers.mediastore.MediaStoreContext.Companion.android
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider.ContentObserver.Flag.Companion.isInsert
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider.ContentObserver.Flag.Companion.isUnknown
import com.flammky.android.medialib.providers.mediastore.api28.MediaStore28
import com.flammky.android.medialib.providers.mediastore.api28.MediaStoreProvider28
import com.flammky.kotlin.common.sync.sync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import kotlin.reflect.KProperty

class MediaStoreAudioProvider28(private val context: MediaStoreContext)
	: MediaStoreProvider28.Audio {


	private val contextHelper = ContextHelper(context.android)
	private val contentResolver = context.android.contentResolver
	private val entityProvider = AudioEntityProvider28(context)

	private val observers = mutableListOf<MediaStoreProvider.ContentObserver>()

	// we can instead just remember it with our entity
	private val rememberMutex = Mutex()
	private var rememberUrisKey = 0
	private var rememberUris = INVALID_rememberUris

	private val ioDispatcher = AndroidCoroutineDispatchers.DEFAULT.io
	private val internalIoScope = CoroutineScope(ioDispatcher + SupervisorJob())

	private val contentObserver = InternalContentObserver()

	init {
		contentResolver.registerContentObserver(uri_audio_external, true, contentObserver)
	}

	@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
	override suspend fun query(): List<MediaStoreAudioEntity28> {
		val key = rememberMutex.withLock { rememberUrisKey }
		return entityProvider.query().also { rememberUris(key, it.map { entity -> entity.uri }) }
	}

	@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
	override suspend fun queryById(id: String): MediaStoreAudioEntity28? {
		return entityProvider.queryById(id)
	}

	@kotlin.jvm.Throws(ReadExternalStoragePermissionException::class)
	override suspend fun queryByUri(uri: Uri): MediaStoreAudioEntity28? {
		return entityProvider.queryByUri(uri)
	}

	override fun observe(observer: MediaStoreProvider.ContentObserver) {
		observers.sync { add(observer) }
	}

	override fun removeObserver(observer: MediaStoreProvider.ContentObserver) {
		observers.sync { removeAll { it === observer } }
	}

	private fun rememberUris(key: Int, uris: List<Uri>) {
		internalIoScope.launch {
			rememberMutex.withLock {
				if (key == rememberUrisKey) {
					rememberUris = uris
				}
			}
		}
	}

	private inner class InternalContentObserver : ContentObserver(null) {

		private val eventLock = Any()
		private var _eventKey by OverflowSafeLong(0) { 0L }
		private val eventKey: Long
			get() {
				return synchronized(eventLock) {
					_eventKey++.also { scheduledEventKey.add(it) }
				}
			}

		private val scheduledEventKey = mutableListOf<Long>()
		private val scheduledEvent = mutableListOf<Pair<Long, () -> Unit>>()

		override fun deliverSelfNotifications(): Boolean = true

		override fun onChange(selfChange: Boolean, uri: Uri?) = onChange(selfChange, uri, 0)

		override fun onChange(selfChange: Boolean, uri: Uri?, flags: Int) {
			if (observers.isEmpty()) return
			if (uri != null) {
				onChange(eventKey, uri, resolveInternalFlag(flags))
			}
		}

		override fun onChange(selfChange: Boolean, uris: MutableCollection<Uri>, flags: Int) {
			if (observers.isEmpty()) return
			uris.forEach { uri ->
				onChange(eventKey, uri, resolveInternalFlag(flags))
			}
		}

		private fun onChange(key: Long, uri: Uri, flag: MediaStoreProvider.ContentObserver.Flag) {
			internalIoScope.launch {
				val resolvedFlag =
					if (flag.isUnknown) {
						resolveUnknownFlag(uri)
					} else {
						flag
					}

				val id = "MediaStore_28_" + uri.toString().takeLastWhile { it.isDigit() }

				if (resolvedFlag.isInsert) {
					queryByUri(uri)?.file?.absolutePath?.let { path ->
						MediaScannerConnection.scanFile(context.android, arrayOf(path), null, null)
					}
				}

				scheduleNotifyEvent(key, id, uri, resolvedFlag)
			}
		}

		// as we want a fair event, we must schedule it
		private fun scheduleNotifyEvent(
			key: Long,
			id: String,
			uri: Uri,
			flag: MediaStoreProvider.ContentObserver.Flag
		) {
			Timber.d("scheduleNotifyEvent: $key, $id, $uri, $flag")

			// function to invoke
			fun notifyObservers() {
				internalIoScope.launch { observers.forEach { it.onChange(id, uri, flag) } }
				Timber.d("scheduleNotifyEvent: $key, sent")
			}
			// sync to eventLock
			synchronized(eventLock) {
				// add this event
				scheduledEvent.add(key to ::notifyObservers)
				val eventKeyToRemove = mutableListOf<Long>()
				val eventIndexToRemove = mutableListOf<Int>()
				// iterate through our scheduler
				scheduledEventKey.forEach { key ->
					scheduledEvent.forEachIndexed { index, pair ->
						if (pair.first == key) {
							pair.second()
							eventIndexToRemove.add(index)
							eventKeyToRemove.add(key)
						}
					}
				}
				// remove the handled event accordingly
				var eventRemoved = 0
				eventKeyToRemove.forEach {
					scheduledEventKey.remove(it)
				}
				eventIndexToRemove.forEach {
					scheduledEvent.removeAt(it - eventRemoved)
					eventRemoved++
				}
			}
		}

		private fun resolveInternalFlag(flags: Int): MediaStoreProvider.ContentObserver.Flag  {
			return when(flags) {
				ContentResolver.NOTIFY_UPDATE -> MediaStoreProvider.ContentObserver.Flag.Update
				ContentResolver.NOTIFY_INSERT -> MediaStoreProvider.ContentObserver.Flag.Insert
				ContentResolver.NOTIFY_DELETE -> MediaStoreProvider.ContentObserver.Flag.Delete
				0 -> MediaStoreProvider.ContentObserver.Flag.Unknown
				else -> MediaStoreProvider.ContentObserver.Flag.Unsupported
			}
		}

		private suspend fun resolveUnknownFlag(uri: Uri): MediaStoreProvider.ContentObserver.Flag {
			val remembered = rememberMutex.withLock { rememberUris }
			return when {
				isContentDeleted(remembered, uri) -> MediaStoreProvider.ContentObserver.Flag.Delete
				isContentInserted(remembered, uri) -> MediaStoreProvider.ContentObserver.Flag.Insert
				isContentUpdated(remembered, uri) -> MediaStoreProvider.ContentObserver.Flag.Update
				else -> MediaStoreProvider.ContentObserver.Flag.Unknown
			}
		}

		private suspend fun isContentDeleted(
			remembered: List<Uri>,
			uri: Uri
		): Boolean {
			// we don't have it remembered
			if (remembered === INVALID_rememberUris || !remembered.contains(uri)) {
				return false
			}
			// no permission either so we can't check for change
			if (!contextHelper.permissions.common.hasReadExternalStorage) {
				return false
			}
			return try {
				queryByUri(uri) == null
			} catch (e: Exception) {
				false
			}
		}

		private suspend fun isContentInserted(
			remembered: List<Uri>,
			uri: Uri
		): Boolean {
			// we don't remember any or we have it remembered already
			if (remembered === INVALID_rememberUris || remembered.contains(uri)) {
				return false
			}
			// no permission either so we can't check for change
			if (!contextHelper.permissions.common.hasReadExternalStorage) {
				return false
			}
			return try {
				queryByUri(uri) != null
			} catch (e: Exception) {
				false
			}
		}

		private suspend fun isContentUpdated(
			remembered: List<Uri>,
			uri: Uri
		): Boolean {
			// we don't have it remembered
			if (remembered === INVALID_rememberUris || !remembered.contains(uri)) {
				return false
			}
			// no permission either so we can't check for change
			if (!contextHelper.permissions.common.hasReadExternalStorage) {
				return false
			}
			return try {
				queryByUri(uri) != null
			} catch (e: Exception) {
				false
			}
		}
	}

	private class OverflowSafeLong(value: Long, private val overflowValue: () -> Long) {
		var value: Long = value
			set(value) {
				field = if (value == Long.MAX_VALUE) overflowValue() else value
			}
	}

	private operator fun OverflowSafeLong.getValue(receiver: Any?, property: KProperty<*>): Long {
		return value
	}

	private operator fun OverflowSafeLong.setValue(receiver: Any?, property: KProperty<*>, value: Long) {
		this.value = value
	}

	companion object {
		private val uri_audio_external: Uri = MediaStore28.Audio.EXTERNAL_CONTENT_URI

		private val INVALID_uri = Uri.parse("InvalidAudio28")
		private val INVALID_rememberUris = listOf(INVALID_uri)
	}
}
