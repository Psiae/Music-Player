package com.flammky.musicplayer.library.dump.localmedia.ui

import android.graphics.Bitmap
import android.os.Looper
import androidx.annotation.MainThread
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers
import com.flammky.android.medialib.common.mediaitem.AudioMetadata
import com.flammky.common.kotlin.coroutines.safeCollect
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.library.BuildConfig
import com.flammky.musicplayer.library.dump.localmedia.data.LocalSongModel
import com.flammky.musicplayer.library.dump.localmedia.data.LocalSongRepository
import com.flammky.musicplayer.library.dump.localmedia.domain.ObservableLocalSongs
import com.flammky.musicplayer.library.dump.media.MediaConnection
import com.flammky.musicplayer.library.dump.util.read
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class LocalSongViewModel @Inject constructor(
	private val authService: AuthService,
	private val dispatcher: AndroidCoroutineDispatchers,
	private val mediaConnection: MediaConnection,
	private val repository: LocalSongRepository,
	private val observableLocalSongs: ObservableLocalSongs,
) : ViewModel() {
	private val artworkCacheStateFlowMap = mutableMapOf<String, MutableStateFlow<Bitmap?>>()

	private val _repoRefresh = mutableStateOf(false)
	private val _localRefresh = mutableStateOf(false)

	val refreshing = derivedStateOf { _repoRefresh.read() || _localRefresh.read() }

	private val _listState = mutableStateOf<ImmutableList<LocalSongModel>>(persistentListOf())
	val listState = _listState.derive()

	private val _loaded = mutableStateOf(false)
	val loadedState = _loaded.derive()

	init {
		viewModelScope.launch {
			observableLocalSongs.collectRefresh().safeCollect {
				_repoRefresh.overwrite(it)
			}
		}
		viewModelScope.launch {
			observableLocalSongs.refresh().join()
			_loaded.overwrite(true)
			observableLocalSongs.collectLocalSongs().safeCollect {
				_listState.overwrite(it)
			}
		}
	}

	fun refresh() {
		viewModelScope.launch {
			if (!_localRefresh.value) {
				_localRefresh.value = true
				val jobs = mutableListOf<Job>()
				observableLocalSongs.refreshAsync().await().forEach { model ->
					// I think we should limit refresh on metadata with validation
					jobs.add(observableLocalSongs.refreshMetadata(model))
				}
				jobs.joinAll()
				_localRefresh.value = false
			}
		}
	}

	override fun onCleared() {
		artworkCacheStateFlowMap.clear()
	}

	// queue should be recognized instead
	fun play(queue: List<LocalSongModel>, index: Int) {
		val user = authService.currentUser
		if (index !in queue.indices || user == null) return
		val cut = queue
		mediaConnection.play(
			user = user,
			cut.map { it.id to it.uri },
			index
		)
	}

	@MainThread
	fun observeArtwork(model: LocalSongModel): StateFlow<Bitmap?> {
		checkInMainLooper {
			"Trying to observeArtwork on worker thread"
		}
		return findArtworkChannelOrCreate(model.id)
	}

	private fun findArtworkChannelOrCreate(id: String): StateFlow<Bitmap?> {
		return (artworkCacheStateFlowMap[id] ?: createAndManageArtworkStateFlow(id)).asStateFlow()
	}

	private fun createAndManageArtworkStateFlow(id: String): MutableStateFlow<Bitmap?> {
		val state = MutableStateFlow<Bitmap?>(null)

		val collectorJob = viewModelScope.launch(dispatcher.main) {
			repository.collectArtwork(id).collect {
				if (!coroutineContext.job.isActive) {
					Timber.d("LocalSongViewModel Art collection ($id) cancelled")
					state.value = null
					throw CancellationException()
				}
				state.value = it
			}
		}

		viewModelScope.launch(dispatcher.main) {
			var delayJob: Job? = null
			state.subscriptionCount.collect { count ->
				if (count < 1) {
					// if subscriptionCount becomes 0 the delayJob should either be null or active
					check(delayJob?.isCancelled != false) {
						"LocalSongViewModel Leak Watcher: delay Job was active when subscriptionCount becomes 0"
					}
					delayJob = launch {
						delay(4000)
						if (isActive) {
							Timber.d("LocalSongViewModel subscription watcher disposing $id")
							collectorJob.cancel()
							state.value = null
							artworkCacheStateFlowMap.remove(id)
							delayJob!!.cancel()
						}
					}
				} else {
					delayJob?.cancel()
				}
			}
		}

		return state.also {
			if (artworkCacheStateFlowMap.put(id, it) != null) {
				if (BuildConfig.DEBUG) error("Trying to create duplicate StateFlow")
			}
		}
	}

	suspend fun collectMetadata(model: LocalSongModel): Flow<AudioMetadata?> {
		return repository.collectMetadata(model.id)
	}

	// is explicit write like this better ?
	@Suppress("NOTHING_TO_INLINE")
	private inline fun <T> MutableState<T>.overwrite(value: T) {
		this.value = value
	}

	private fun <T> State<T>.derive(): State<T> = derive { it }

	private fun <T> State<T>.derive(calculation: (T) -> T): State<T> {
		return derivedStateOf { calculation(value) }
	}

	@kotlin.jvm.Throws(IllegalStateException::class)
	private inline fun checkInMainLooper(lazyMsg: () -> Any) {
		check(Looper.myLooper() == Looper.getMainLooper(), lazyMsg)
	}
}


