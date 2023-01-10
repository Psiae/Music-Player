package com.flammky.musicplayer.base.media.r

import android.content.Context
import android.graphics.Bitmap
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.temp.cache.lru.DefaultLruCache
import com.flammky.kotlin.common.lazy.LazyConstructor
import com.flammky.kotlin.common.lazy.LazyConstructor.Companion.valueOrNull
import com.flammky.musicplayer.base.media.r.RealMediaMetadataCacheRepository.MapObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class RealMediaMetadataCacheRepository(
	private val context: Context,
	private val dispatchers: AndroidCoroutineDispatchers
) : MediaMetadataCacheRepository {

	private val rwl = ReentrantReadWriteLock()

	// we should confine between `observed` and not
	private val metadataLRU = DefaultLruCache<String, MediaMetadata>(150)
	private val metadataObservers = mutableMapOf<String, MutableList<MapObserver<String, MediaMetadata>>>()

	// should define abstract class instead
	private val artworkLRU = DefaultLruCache<String, Any>(150000000) { _, art ->
		if (art is Bitmap) {
			art.allocationByteCount.toLong()
		} else {
			1
		}
	}
	private val artworkObservers = mutableMapOf<String, MutableList<MapObserver<String, Any>>>()

	private val mainScope = CoroutineScope(dispatchers.main + SupervisorJob())

	override fun getMetadata(id: String): MediaMetadata? {
		return metadataLRU[id]
	}

	override fun observeMetadata(id: String): Flow<MediaMetadata?> = flow {
		if (id == "") {
			return@flow
		}
		val channel = Channel<MediaMetadata?>()
		val observer = MapObserver<String, MediaMetadata> { key, value ->
			check(key == id)
			channel.send(value)
		}
		addMetadataObserver(id, observer)
		emit(getMetadata(id))
		runCatching {
			for (metadata in channel) {
				emit(metadata)
			}
		}.onFailure { ex ->
			if (ex !is CancellationException) throw ex
			removeMetadataObserver(id, observer)
		}
	}

	private fun addMetadataObserver(id: String, observer: MapObserver<String, MediaMetadata>) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			metadataObservers.getOrPut(id) { mutableListOf() }.add(observer)
		}
	}

	private fun removeMetadataObserver(id: String, observer: MapObserver<String, MediaMetadata>) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			metadataObservers[id]?.remove(observer)
		}
	}

	override fun provideMetadata(id: String, metadata: MediaMetadata) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			metadataLRU.put(id, metadata)
			dispatchMetadataChange(id, metadata)
		}
	}

	override fun silentProvideMetadata(id: String, metadata: MediaMetadata) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			metadataLRU.put(id, metadata)
		}
	}


	override fun evictMetadata(id: String) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			metadataLRU.remove(id)?.let { dispatchArtworkChange(id, null) }
		}
	}

	override fun silentEvictMetadata(id: String) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			metadataLRU.remove(id)
		}
	}

	override fun getArtwork(id: String): Any? {
		return artworkLRU[id]
	}

	override fun observeArtwork(id: String): Flow<Any?> = flow {
		if (id == "" || id[0] == '_') {
			return@flow
		}
		val channel = Channel<Any?>()
		val observer = MapObserver<String, Any> { key, value ->
			check(key == id)
			channel.send(value)
		}
		addArtworkObserver(id, observer)
		emit(getArtwork(id))
		runCatching {
			for (art in channel) emit(art)
		}.onFailure { ex ->
			if (ex !is CancellationException) throw ex
			removeArtworkObserver(id, observer)
		}
	}

	private fun addArtworkObserver(id: String, observer: MapObserver<String, Any>) {
		if (id == "") {
			return
		}
		rwl.write {
			artworkObservers.getOrPut(id) { mutableListOf() }.add(observer)
		}
	}

	private fun removeArtworkObserver(id: String, observer: MapObserver<String, Any>) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			artworkObservers[id]?.remove(observer)
		}
	}

	override fun provideArtwork(id: String, artwork: Any) {
		if (id == "" || id[0] == '_') {
			return
		}
		Timber.d("provideArtwork: $id $artwork")
		rwl.write {
			artworkLRU.put(id, artwork)
			dispatchArtworkChange(id, artwork)
		}
	}

	override fun evictArtwork(id: String) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			artworkLRU.remove(id)
				?.let { dispatchArtworkChange(id, null) }
			// temporary
			val n = id.removeSuffix("_raw") + "_notification"
			artworkLRU.remove(n)
				?.let { dispatchArtworkChange(n, null) }
		}
	}

	override fun silentProvideArtwork(id: String, artwork: Any) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			artworkLRU.put(id, artwork)
		}
	}

	override fun silentEvictArtwork(id: String) {
		if (id == "" || id[0] == '_') {
			return
		}
		rwl.write {
			artworkLRU.remove(id)
			// temporary
			artworkLRU.remove(id.removeSuffix("_raw") + "_notification")
		}
	}

	private fun dispatchMetadataChange(id: String, metadata: MediaMetadata?) {
		metadataObservers[id]?.toList()?.let { observers ->
			mainScope.launch { observers.forEach { it.onChanged(id, metadata) } }
		}
	}

	private fun dispatchArtworkChange(id: String, artwork: Any?) {
		artworkObservers[id]?.toList()?.let { observers ->
			mainScope.launch { observers.forEach { it.onChanged(id, artwork) } }
		}
	}

	private fun <R> Mutex.withLockBlocking(block: () -> R): R {
		return if (tryLock()) {
			try {
				block()
			} finally {
				unlock()
			}
		} else {
			runBlocking {
				withLock(action = block)
			}
		}
	}

	private fun interface MapObserver<K: Any, V> {
		suspend fun onChanged(key: K, value: V?)
	}

	companion object {
		private val SINGLETON = LazyConstructor<RealMediaMetadataCacheRepository>()
		fun provide(ctx: Context, dispatchers: AndroidCoroutineDispatchers) = SINGLETON.construct {
			RealMediaMetadataCacheRepository(ctx, dispatchers)
		}

		fun get() = SINGLETON.valueOrNull() ?: error("RealMediaConnectionRepository was not provided")
	}
}
