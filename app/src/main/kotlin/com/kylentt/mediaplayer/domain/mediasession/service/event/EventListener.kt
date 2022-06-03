package com.kylentt.mediaplayer.domain.mediasession.service.event

import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateReady
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import timber.log.Timber

@MainThread
class MusicLibraryListener(
	private val service: MusicLibraryService
) {

	private val playerListeners = mutableListOf<Pair<Player, Player.Listener>>()

	private val playerStateIdleListener = mutableListOf<(Player) -> Unit>()
	private val playerStateBufferingListener = mutableListOf<(Player) -> Unit>()
	private val playerStateReadyListener = mutableListOf<(Player) -> Unit>()
	private val playerStateEndedListener = mutableListOf<(Player) -> Unit>()

	private val playerListenerImpl = object : Player.Listener {

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			super.onPlayWhenReadyChanged(playWhenReady, reason)
			handlePlayWhenReadyChanged(currentMediaSession)
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			super.onPlaybackStateChanged(playbackState)
			handlePlayerStateChanged(currentMediaSession, playbackState)
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			super.onMediaItemTransition(mediaItem, reason)
			handleMediaItemTransition(currentMediaSession, reason)
		}

		override fun onRepeatModeChanged(repeatMode: Int) {
			super.onRepeatModeChanged(repeatMode)
			handleRepeatModeChanged(currentMediaSession, repeatMode)
		}

		override fun onPlayerError(error: PlaybackException) {
			super.onPlayerError(error)
			handlePlayerError(currentMediaSession, error)
		}
	}

	private var isRegistered: Boolean = false

	val currentMediaSession: MediaSession
		get() = service.currentMediaSession

	val currentPlayer: Player
		get() = service.currentMediaSession.player

	val defaultPlayerLister: Player.Listener
		get() = playerListenerImpl

	val isPlayerStateIdle: Boolean
		get() = currentPlayer.playbackState.isStateIdle()

	val isPlayerStateBuffering: Boolean
		get() = currentPlayer.playbackState.isStateBuffering()

	val isPlayerStateReady: Boolean
		get() = currentPlayer.playbackState.isStateReady()

	val isPlayerStateEnded: Boolean
		get() = currentPlayer.playbackState.isStateEnded()

	val isPlayerPlaying: Boolean
		get() = currentPlayer.isPlaying

	val isPlayerPlayWhenReady: Boolean
		get() = currentPlayer.playWhenReady

	init {
		checkNotNull(service.baseContext) {
			"Make Sure to Initialize ${this::class.java.simpleName} lazily at least service.onCreate()"
		}
	}

	@MainThread
	fun init(stopSelf: Boolean) {
		checkMainThread()
		if (!isRegistered) {
			startListener()
			if (stopSelf) service.registerOnDestroyCallback { stopListener() }
		}
	}

	private fun startListener() {
		registerPlayerListener(currentMediaSession.player, defaultPlayerLister)
		isRegistered = true
	}

	private fun stopListener() {
		unregisterPlayerListener(currentMediaSession.player, defaultPlayerLister)
		isRegistered = false
	}

	private var playWhenReadyChangedJob = Job().job
	private fun handlePlayWhenReadyChanged(session: MediaSession) {
		playWhenReadyChangedJob.cancel()
		playWhenReadyChangedJob = service.mainScope.launch {
			service.mediaEventHandler.handlePlayerPlayWhenReadyChanged(session)
		}
	}

	private var itemTransitionJob = Job().job
	private fun handleMediaItemTransition(session: MediaSession, reason: Int) {
		itemTransitionJob.cancel()
		itemTransitionJob = service.mainScope.launch {
			service.mediaEventHandler.handlePlayerMediaItemChanged(currentMediaSession, reason)
		}
	}

	private var playerErrorJob = Job().job
	private fun handlePlayerError(session: MediaSession, error: PlaybackException) {
		playerErrorJob.cancel()
		playerErrorJob = service.mainScope.launch {
				service.mediaEventHandler.handlePlayerError(session, error)
			}
	}

	private var playerStateChangedJob = Job().job
	private fun handlePlayerStateChanged(session: MediaSession, playbackState: @Player.State Int) {
		playerStateChangedJob.cancel()
		playerStateChangedJob = service.mainScope.launch {
			service.mediaEventHandler.handlePlayerStateChanged(session)
		}
		when (playbackState) {
			Player.STATE_READY -> playerStateReadyListener.forEachClear { it(currentPlayer) }
			Player.STATE_BUFFERING -> playerStateReadyListener.forEachClear { it(currentPlayer) }
			Player.STATE_IDLE -> playerStateReadyListener.forEachClear { it(currentPlayer) }
			Player.STATE_ENDED -> playerStateEndedListener.forEachClear { it(currentPlayer) }
		}
	}

	private var repeatModeChangedJob = Job().job
	private fun handleRepeatModeChanged(session: MediaSession, mode: Int) {
		repeatModeChangedJob.cancel()
		repeatModeChangedJob = service.mainScope.launch {
			service.mediaEventHandler.handlePlayerRepeatModeChanged(session)
		}
	}

	private inline fun elseFalse(condition: Boolean, block: () -> Unit): Boolean {
		return if (condition) {
			block()
			condition
		} else {
			false
		}
	}

	fun registerPlayerListener(player: Player, listener: Player.Listener) {
		player.addListener(listener)
		this.playerListeners.add(player to listener)
	}

	fun unregisterPlayerListener(player: Player) {
		this.playerListeners.removeAll {
			elseFalse(it.first === player) {
				it.first.removeListener(it.second)
			}
		}
	}

	fun unregisterPlayerListener(listener: Player.Listener) {
		this.playerListeners.removeAll {
			elseFalse(listener === playerListeners) {
				it.first.removeListener(it.second)
			}
		}
	}

	fun unregisterPlayerListener(player: Player, listener: Player.Listener) {
		this.playerListeners.removeAll {
			elseFalse(it.first === player && it.second === listener) {
				it.first.removeListener(it.second)
			}
		}
	}

	fun unregisterAllListener(obj: Any) {
		checkMainThread()
		if (playerListeners.isEmpty()) return
		playerListeners.forEachClear { it.first.removeListener(it.second) }
		Timber.d("$obj made call to unregisterAllListener")
	}

	fun whenPlayerStateIdle(what: (Player) -> Unit) {
		if (isPlayerStateIdle) {
			return what(currentPlayer)
		}
		playerStateIdleListener.add(what)
	}

	fun whenPlayerStateBuffering(what: (Player) -> Unit) {
		if (isPlayerStateBuffering) {
			return what(currentPlayer)
		}
		playerStateBufferingListener.add(what)
	}

	fun whenPlayerStateReady(what: (Player) -> Unit) {
		if (!isPlayerStateReady) {
			return what(currentPlayer)
		}
		playerStateReadyListener.add(what)
	}

	fun whenPlayerStateEnded(what: (Player) -> Unit) {
		if (!isPlayerStateEnded) {
			return what(currentPlayer)
		}
		playerStateEndedListener.add(what)
	}
}
