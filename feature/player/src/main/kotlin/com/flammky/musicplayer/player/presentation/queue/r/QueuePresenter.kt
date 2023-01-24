package com.flammky.musicplayer.player.presentation.queue.r

import android.graphics.Bitmap
import com.flammky.android.medialib.common.mediaitem.MediaMetadata
import com.flammky.android.medialib.temp.image.ArtworkProvider
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.media.MetadataProvider
import com.flammky.musicplayer.base.media.mediaconnection.playback.PlaybackConnection
import com.flammky.musicplayer.base.media.r.MediaMetadataCacheRepository
import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.core.common.sync
import com.flammky.musicplayer.core.concurrent.inMainLooper
import com.flammky.musicplayer.player.presentation.controller.PlaybackController
import com.flammky.musicplayer.player.presentation.queue.QueuePresenter
import com.flammky.musicplayer.player.presentation.r.RealPlaybackController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

// better way is probably to inject supplier instead
internal class ExpectQueuePresenter(
    private val authService: AuthService,
    private val playbackConnection: PlaybackConnection,
    private val artworkProvider: ArtworkProvider,
    private val metadataProvider: MetadataProvider,
    private val metadataCacheRepository: MediaMetadataCacheRepository
) : QueuePresenter {

    private var actual: Actual? = null

    override fun init(viewModel: QueuePresenter.ViewModel) {
        check(inMainLooper())
        if (actual is Actual) {
            return
        }
        actual = Actual(
            viewModel = viewModel,
            authService = authService,
            playbackConnection = playbackConnection,
            artworkProvider = artworkProvider,
            metadataProvider = metadataProvider,
            metadataCacheRepository = metadataCacheRepository
        )
    }

    override fun dispose() {
        check(inMainLooper())
        actual?.dispose()
    }

    override val disposed: Boolean
        get() {
            check(inMainLooper())
            return actual?.disposed == true
        }

    override val auth: QueuePresenter.Auth
        get() = actual?.auth
            ?: error("")

    override val playback: QueuePresenter.Playback
        get() = actual?.playback
            ?: error("")

    override val repo: QueuePresenter.Repo
        get() = actual?.repo
            ?: error("")
}

private class Actual(
    private val viewModel: QueuePresenter.ViewModel,
    private val authService: AuthService,
    private val playbackConnection: PlaybackConnection,
    private val artworkProvider: ArtworkProvider,
    private val metadataProvider: MetadataProvider,
    private val metadataCacheRepository: MediaMetadataCacheRepository
) : QueuePresenter {

    private val coroutineScope = CoroutineScope(Dispatchers.Main + viewModel.coroutineSupervisor)
    private val _lock = Any()
    private var _disposed = false

    override val disposed: Boolean
        get() = _disposed

    override fun init(
        viewModel: QueuePresenter.ViewModel
    ) = error("")

    override fun dispose() {
        check(inMainLooper())
        _disposed = true
        coroutineScope.cancel()
    }

    override val auth: QueuePresenter.Auth = object : QueuePresenter.Auth {

        override fun observeUser(): Flow<User?> {
            check(inMainLooper())
            if (_disposed) return flow {  }
            return authService.observeCurrentUser()
        }
    }

    override val playback: QueuePresenter.Playback = object : QueuePresenter.Playback {
        private val controllers = mutableMapOf<User, MutableList<PlaybackController>>()
        override fun createController(user: User): PlaybackController {
            check(inMainLooper())
            val supervisor = SupervisorJob(coroutineScope.coroutineContext.job)
            return RealPlaybackController(
                user,
                CoroutineScope(coroutineScope.coroutineContext + supervisor),
                playbackConnection,
                disposeHandle = { controller ->
                    controllers.sync(_lock) {
                        get(user)?.remove(controller)
                    }
                }
            ).also { controller ->
                if (_disposed) {
                    controller.dispose()
                    return@also
                }
                controllers.sync(_lock) {
                    getOrPut(user) { mutableListOf() }.add(controller)
                }
            }
        }
    }

    override val repo: QueuePresenter.Repo = object : QueuePresenter.Repo {

        override fun observeArtwork(id: String): Flow<Any?> {
            check(inMainLooper())
            if (_disposed) {
                return flow {  }
            }
            return flow {
                val supervisor = Job()
                val sf = MutableSharedFlow<Any?>(1)
                    .apply msf@ {
                        coroutineScope.launch(supervisor) {
                            artworkProvider
                                .request(ArtworkProvider.Request.Builder(id, Bitmap::class.java).build())
                            metadataCacheRepository.observeArtwork(id + "_raw").collect {
                                this@msf.emit(it)
                            }
                        }
                    }
                runCatching collector@ {
                    sf.collect(this@collector)
                }.onFailure { ex ->
                    supervisor.cancel()
                    if (ex !is CancellationException) throw ex
                }
            }
        }

        override fun observeMetadata(id: String): Flow<MediaMetadata?> {
            check(inMainLooper())
            if (_disposed) {
                return flow {  }
            }
            return flow {
                val supervisor = Job()
                val sf = MutableSharedFlow<MediaMetadata?>(1)
                    .apply msf@ {
                        coroutineScope.launch(supervisor) {
                            metadataProvider
                                .requestAsync(id)
                            metadataCacheRepository.observeMetadata(id).collect {
                                this@msf.emit(it)
                            }
                        }
                    }
                runCatching collector@ {
                    sf.collect(this@collector)
                }.onFailure { ex ->
                    supervisor.cancel()
                    if (ex !is CancellationException) throw ex
                }
            }
        }
    }
}