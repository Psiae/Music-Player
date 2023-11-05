package dev.dexsr.klio.player.presentation.root.bw

import com.flammky.musicplayer.base.user.User
import com.flammky.musicplayer.player.presentation.main.PlaybackControlViewModel
import dev.dexsr.klio.player.presentation.root.RootCompactPlaybackController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest

internal class OldRootCompactPlaybackController(
    private val user: User,
    private val viewModel: PlaybackControlViewModel
) : RootCompactPlaybackController {

    private val playbackController = viewModel.createUserPlaybackController(user = user)

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun currentlyPlayingMediaIdAsFlow(): Flow<String?> {
        return flow {
            val po = playbackController.createPlaybackObserver()
            try {
                po.createQueueCollector()
                    .apply {
                        startCollect().join()
                        queueStateFlow
                            .mapLatest { q ->
                                if (q.currentIndex == -1) return@mapLatest null
                                q.list[q.currentIndex]
                            }
                            .collect(this@flow)
                    }
            } finally {
                po.dispose()
            }
        }
    }

    fun dispose() {
        playbackController.dispose()
    }
}