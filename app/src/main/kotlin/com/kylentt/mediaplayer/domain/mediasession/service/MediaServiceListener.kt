package com.kylentt.mediaplayer.domain.mediasession.service

import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.delegates.LockMainThread
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateReady
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import timber.log.Timber

@MainThread
interface MediaServiceListener {
	val currentMediaSession: MediaSession
	val currentPlayer: Player
	val defaultPlayerLister: Player.Listener

	val isPlayerStateIdle: Boolean
	val isPlayerStateBuffering: Boolean
	val isPlayerStateReady: Boolean
	val isPlayerStateEnded: Boolean
	val isPlayerPlaying: Boolean
	val isPlayerPlayWhenReady: Boolean

	fun registerPlayerListener(player: Player, listener: Player.Listener)

	fun unregisterPlayerListener(player: Player)
	fun unregisterPlayerListener(listener: Player.Listener)
	fun unregisterPlayerListener(player: Player, listener: Player.Listener)
	fun unregisterAllListener(obj: Any)

	fun whenPlayerStateIdle(what: (Player) -> Unit)
	fun whenPlayerStateBuffering(what: (Player) -> Unit)
	fun whenPlayerStateReady(what: (Player) -> Unit)
	fun whenPlayerStateEnded(what: (Player) -> Unit)
}

@MainThread
class MediaServiceListenerImpl(
	private val service: MediaService
): MediaServiceListener {

	private val playerListeners = mutableListOf<Pair<Player, Player.Listener>>()

	private val playerStateIdleListener = mutableListOf<(Player) -> Unit>()
	private val playerStateBufferingListener = mutableListOf<(Player) -> Unit>()
	private val playerStateReadyListener = mutableListOf<(Player) -> Unit>()
	private val playerStateEndedListener = mutableListOf<(Player) -> Unit>()

	private val playerListenerImpl = object : Player.Listener {

		override fun onPlaybackStateChanged(playbackState: Int) {
			super.onPlaybackStateChanged(playbackState)
			if (!playbackState.isOngoing()) {
				notificationProvider.considerForegroundService(currentMediaSession)
			}
			handlePlayerStateChanged(playbackState)
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			super.onMediaItemTransition(mediaItem, reason)
			handleMediaItemTransition(mediaItem ?: MediaItem.EMPTY, reason)
		}

		override fun onPlayerError(error: PlaybackException) {
			super.onPlayerError(error)
			handlePlayerError(error)
		}

		override fun onRepeatModeChanged(repeatMode: Int) {
			super.onRepeatModeChanged(repeatMode)
			handleRepeatModeChanged(repeatMode)
		}
	}

	val notificationProvider
		get() = service.notificationProvider

	override val currentMediaSession: MediaSession
		get() = service.currentMediaSession

	override val currentPlayer: Player
		get() = service.currentSessionPlayer

	override val defaultPlayerLister: Player.Listener
		get() = playerListenerImpl

	override val isPlayerStateIdle: Boolean
		get() = currentPlayer.playbackState.isStateIdle()

	override val isPlayerStateBuffering: Boolean
		get() = currentPlayer.playbackState.isStateBuffering()

	override val isPlayerStateReady: Boolean
		get() = currentPlayer.playbackState.isStateReady()

	override val isPlayerStateEnded: Boolean
		get() = currentPlayer.playbackState.isStateEnded()

	override val isPlayerPlaying: Boolean
		get() = currentPlayer.isPlaying

	override val isPlayerPlayWhenReady: Boolean
		get() = currentPlayer.playWhenReady

	init {
		checkNotNull(service.baseContext) {
			"Make Sure to Initialize ${this::class.java.simpleName} lazily after super.onCreate()"
		}
	}

	override fun registerPlayerListener(player: Player, listener: Player.Listener) {
		player.addListener(listener)
		this.playerListeners.add(player to listener)
	}

	override fun unregisterPlayerListener(player: Player) {
		this.playerListeners.removeAll {
			elseFalse(it.first === player) {
				it.first.removeListener(it.second)
			}
		}
	}

	override fun unregisterPlayerListener(listener: Player.Listener) {
		this.playerListeners.removeAll {
			elseFalse(listener === playerListeners) {
				it.first.removeListener(it.second)
			}
		}
	}

	override fun unregisterPlayerListener(player: Player, listener: Player.Listener) {
		this.playerListeners.removeAll {
			elseFalse(it.first === player && it.second === listener) {
				it.first.removeListener(it.second)
			}
		}
	}

	override fun unregisterAllListener(obj: Any) {
		checkMainThread()
		if (playerListeners.isEmpty()) return
		playerListeners.forEachClear { it.first.removeListener(it.second) }
		Timber.d("$obj made call to unregisterAllListener")
	}

	override fun whenPlayerStateIdle(what: (Player) -> Unit) {
		if (isPlayerStateIdle) {
			return what(currentPlayer)
		}
		playerStateIdleListener.add(what)
	}

	override fun whenPlayerStateBuffering(what: (Player) -> Unit) {
		if (isPlayerStateBuffering) {
			return what(currentPlayer)
		}
		playerStateBufferingListener.add(what)
	}

	override fun whenPlayerStateReady(what: (Player) -> Unit) {
		if (!isPlayerStateReady) {
			return what(currentPlayer)
		}
		playerStateReadyListener.add(what)
	}

	override fun whenPlayerStateEnded(what: (Player) -> Unit) {
		if (!isPlayerStateEnded) {
			return what(currentPlayer)
		}
		playerStateEndedListener.add(what)
	}

	private var playerErrorJob by LockMainThread(Job().job)
	private fun handlePlayerError(error: PlaybackException) {
		playerErrorJob.cancel()
		playerErrorJob = service.mainScope.launch {
			service.sessionEventHandler.handlePlayerError(currentPlayer, error)
		}
	}

	private var playerStateChangedJob by LockMainThread(Job().job)
	private fun handlePlayerStateChanged(playbackState: @Player.State Int) {
		playerStateChangedJob.cancel()
		playerStateChangedJob = service.mainScope.launch {
			service.sessionEventHandler.handlePlayerStateChanged(playbackState)
			when (playbackState) {
				Player.STATE_READY -> playerStateReadyListener.forEachClear { it(currentPlayer) }
				Player.STATE_BUFFERING -> playerStateReadyListener.forEachClear { it(currentPlayer) }
				Player.STATE_IDLE -> playerStateReadyListener.forEachClear { it(currentPlayer) }
				Player.STATE_ENDED -> playerStateEndedListener.forEachClear { it(currentPlayer) }
			}
		}
	}

	private var mediaItemTransitionJob by LockMainThread(Job().job)
	private fun handleMediaItemTransition(item: MediaItem, reason: Int) {
		mediaItemTransitionJob.cancel()
		mediaItemTransitionJob = service.mainScope.launch {
			service.sessionEventHandler.handlePlayerMediaItemChanged(item, reason)
		}
	}

	private var repeatModeChangedJob by LockMainThread(Job().job)
	private fun handleRepeatModeChanged(mode: Int) {
		repeatModeChangedJob.cancel()
		repeatModeChangedJob = service.mainScope.launch {
			service.sessionEventHandler.handlePlayerRepeatModeChanged(mode)
		}
	}

	private inline fun elseFalse(condition: Boolean, block: () -> Unit): Boolean {
		return if (condition) {
			block()
			true
		} else {
			false
		}
	}
}
