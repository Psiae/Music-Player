package com.flammky.musicplayer.base.media.r

import android.content.Context
import android.graphics.Bitmap
import com.flammky.android.content.context.ContextHelper
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.temp.cache.lru.DefaultLruCache
import com.flammky.kotlin.common.lazy.LazyConstructor
import com.flammky.kotlin.common.lazy.LazyConstructor.Companion.valueOrNull
import com.flammky.musicplayer.base.media.r.RealMediaMetadataCacheRepository.MapObserver
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write

class RealMediaMetadataCacheRepository(
	private val contextHelper: ContextHelper,
	private val dispatchers: AndroidCoroutineDispatchers
) : MediaMetadataCacheRepository {

	private val rwl = ReentrantReadWriteLock()

	// we should confine between `observed` and not
	private val metadataLRU = DefaultLruCache<String, MediaMetadata>(200)
	private val metadataObservers = mutableMapOf<String, MutableList<MapObserver<String, MediaMetadata>>>()

	// should define abstract class instead
	private val artworkLRU = DefaultLruCache<String, Any>(
		(contextHelper.device.memInfo.totalMem / 10).coerceAtMost(300_000_000)
	) { _, art ->
		if (art is Bitmap) {
			art.allocationByteCount.toLong()
		} else {
			1
		}
	}
	private val artworkObservers = mutableMapOf<String, MutableList<MapObserver<String, Any>>>()

	private val mainScope = CoroutineScope(dispatchers.computation.limitedParallelism(1) + SupervisorJob())

	override fun getMetadata(id: String): MediaMetadata? {
		return metadataLRU[id]
	}

	override fun observeMetadata(id: String): Flow<MediaMetadata?> = flow {
		if (id == "") {
			return@flow
		}
		val sf = MutableSharedFlow<MediaMetadata?>(1)
		val observer = MapObserver<String, MediaMetadata> { key, value ->
			check(key == id)
			sf.emit(value)
		}
		mainScope.launch {
			rwl.write {
				addMetadataObserver(id, observer)
				getMetadata(id)
			}.also { cached ->
				sf.emit(cached)
			}
		}
		runCatching {
			sf.collect(this)
		}.onFailure { ex ->
			removeMetadataObserver(id, observer)
			if (ex !is CancellationException) throw ex
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
		val sf = MutableSharedFlow<Any?>(1)
		val observer = MapObserver<String, Any> { key, value ->
			check(key == id)
			sf.emit(value)
		}
		mainScope.launch {
			rwl.write {
				addArtworkObserver(id, observer)
				getArtwork(id)
			}.also { cached ->
				sf.emit(cached)
			}
		}
		runCatching {
			sf.collect(this)
		}.onFailure { ex ->
			removeArtworkObserver(id, observer)
			if (ex !is CancellationException) throw ex
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
			RealMediaMetadataCacheRepository(ContextHelper(ctx), dispatchers)
		}

		fun get() = SINGLETON.valueOrNull() ?: error("RealMediaConnectionRepository was not provided")
	}
}
