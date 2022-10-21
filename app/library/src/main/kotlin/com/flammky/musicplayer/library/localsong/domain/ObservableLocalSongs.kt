package com.flammky.musicplayer.library.localsong.domain

import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.providers.mediastore.MediaStoreProvider
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.library.localsong.data.LocalSongModel
import com.flammky.musicplayer.library.localsong.data.LocalSongRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

// TODO
interface ObserveLocalSongs {
	fun get(scope: CoroutineScope): ObservableLocalSongs
}

interface ObservableLocalSongs {
	fun collectLocalSongs(): Flow<ImmutableList<LocalSongModel>>
	fun collectRefresh(): Flow<Boolean>
	suspend fun refresh()

	fun release()
}

class RealObservableLocalSongs(
	private val repository: LocalSongRepository,
	// we should have interface on repository instead
	private val mediaStore: MediaStoreProvider,
	private val dispatchers: AndroidCoroutineDispatchers
) : ObservableLocalSongs {

	private val ioScope = CoroutineScope(dispatchers.io + SupervisorJob())
	private val rememberMutex = Mutex()
	private var _rememberList: ImmutableList<LocalSongModel> = persistentListOf()
	private val scheduledRefresh = mutableListOf<Any?>()

	private val refreshStateMSF = MutableStateFlow(false)
	private val localSongsMSF = MutableStateFlow<ImmutableList<LocalSongModel>>(persistentListOf())

	private var _rememberRefreshing = false

	private val scheduleMutex = Mutex()

	private suspend fun sendUpdate(
		list: ImmutableList<LocalSongModel>,
		refreshing: Boolean
	) {
		rememberMutex.withLock {
			_rememberList = list
			_rememberRefreshing = refreshing
		}
		withContext(dispatchers.main) {
			localSongsMSF.value = list
			refreshStateMSF.value = refreshing
		}
		Timber.d("ObserveLocalSongs sendUpdate, refresh: $refreshing, ${refreshStateMSF.value} contents: $list")
	}

	private suspend fun getScheduledSize() = scheduleMutex.withLock { scheduledRefresh.size }
	private suspend fun getRememberedList() = rememberMutex.withLock { _rememberList }

	private suspend fun doScheduledRefresh(id: String? = null): ImmutableList<LocalSongModel> {
		if (id == null) {
			while (true) {
				val size = getScheduledSize()
				val get = repository.getModelsAsync().await().toPersistentList()
				val remains = scheduleMutex.withLock {
					scheduledRefresh.drop(size).let { leftover ->
						scheduledRefresh.clear()
						scheduledRefresh.addAll(leftover)
						scheduledRefresh
					}.also { Timber.d("doScheduledRefresh, refreshed $size at once") }
				}
				if (remains.isEmpty()) return get
			}
		} else {
			return doScheduledRefresh(null)
			// TODO
		}
	}

	private suspend fun scheduleRefresh(id: String? = null) {
		ioScope.launch {
			scheduleMutex.withLock { scheduledRefresh.add(id) }
			val remembered = rememberMutex.withLock {
				if (_rememberRefreshing) return@launch
				_rememberRefreshing = true
				_rememberList
			}
			sendUpdate(remembered, true)
			sendUpdate(doScheduledRefresh(id), false)
		}
	}

	private val observer = MediaStoreProvider.ContentObserver { id, uri, flag ->
		Timber.d("ObserveCurrentAvailable $id, $uri, $flag")
		ioScope.launch { scheduleRefresh(id) }
	}

	override fun collectLocalSongs(): Flow<ImmutableList<LocalSongModel>> = localSongsMSF.asStateFlow()
	override fun collectRefresh(): Flow<Boolean> = refreshStateMSF

	override suspend fun refresh() {
		rememberMutex.withLock {
			if (_rememberRefreshing) {
				Timber.d("ObserveLocalSongs rememberRefreshing was true, ignored")
				return
			}
		}
		coroutineScope {
			refreshStateMSF.value = true
			scheduleRefresh()
		}
	}

	init {
		ioScope.launch {
			val observed = AtomicBoolean(false)
			localSongsMSF.subscriptionCount.safeCollect {
				if (it > 0 && observed.compareAndSet(false, true)) {
					mediaStore.audio.observe(observer)
					ioScope.launch { scheduleRefresh() }
				} else if (it == 0 && observed.compareAndSet(true, false)) {
					mediaStore.audio.removeObserver(observer)
				}
			}
		}
	}

	override fun release() {
		ioScope.cancel()
		mediaStore.audio.removeObserver(observer)
	}
}
