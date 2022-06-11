package com.kylentt.mediaplayer.domain.mediasession.service.event

import androidx.annotation.MainThread
import androidx.lifecycle.*
import androidx.lifecycle.Lifecycle.Event.*
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isOngoing
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateBuffering
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateEnded
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateIdle
import com.kylentt.mediaplayer.core.exoplayer.PlayerExtension.isStateReady
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import timber.log.Timber

class MusicLibraryServiceListener(
	private val service: MusicLibraryService,
	private val mediaEventHandler: MusicLibraryEventHandler,
	private val lifecycleOwner: LifecycleOwner
) {

	private val playerListeners = mutableListOf<Pair<Player, Player.Listener>>()

	private val playerListenerImpl = PlayerListenerImpl()
	private val lifecycleObserverImpl = LifecycleObserverImpl()

	private var stopSelf: Boolean = false
	private var releaseSelf: Boolean = false

	private val immediateScope
		get() = service.mainImmediateScope

	private val mainScope
		get() = service.mainScope

	private val ioScope
		get() = service.mainScope

	var state: STATE = STATE.INITIALIZED
		private set

	val defaultPlayerLister: Player.Listener
		get() = playerListenerImpl

	val currentMediaSession: MediaSession
		get() = service.currentMediaSession

	val currentPlayer: Player
		get() = currentMediaSession.player

	val currentPlayerMediaItem: MediaItem?
		get() = currentPlayer.currentMediaItem

	val isPlayerStateIdle: Boolean
		get() = currentPlayer.playbackState.isStateIdle()

	val isPlayerStateBuffering: Boolean
		get() = currentPlayer.playbackState.isStateBuffering()

	val isPlayerStateReady: Boolean
		get() = currentPlayer.playbackState.isStateReady()

	val isPlayerStateEnded: Boolean
		get() = currentPlayer.playbackState.isStateEnded()

	val isPlayerOngoing: Boolean
		get() = currentPlayer.playbackState.isOngoing()

	val isPlayerPlaying: Boolean
		get() = currentPlayer.isPlaying

	val isPlayerPlayWhenReady: Boolean
		get() = currentPlayer.playWhenReady

	private fun startDefaultListener() {
		startServiceEventListener()

		service.registerOnPlayerChanged { old: Player?, new: Player ->
			val isChanged = old !== new

			checkState(isChanged) {
				"received onPlayerChanged callback when there is no change"
			}

			if (old == null) {
				return@registerOnPlayerChanged registerPlayerListener(new, playerListenerImpl)
			}

			val rem = unregisterPlayerListener(old, playerListenerImpl)

			checkState(rem) {
				"playerListenerImpl was not registered for $old, possible inconsistency"
			}

			val rem2 = playerListeners.find { it.second === playerListenerImpl} == null

			checkState(rem2) {
				"playerListenerImpl was not unRegistered, possible inconsistency" +
					"\n${playerListeners}"
			}

			registerPlayerListener(new, playerListenerImpl)
		}
	}

	private fun startServiceEventListener() {
		lifecycleOwner.lifecycle.addObserver(lifecycleObserverImpl)
	}

	private var eventCollectorJob = Job().job
	private fun launchEventCollector() {
		eventCollectorJob.cancel()
		eventCollectorJob = service.mainImmediateScope.launch {
			service.serviceEventSF.collect {
				serviceEventChanged(it)
			}
		}
	}

	private fun stopDefaultListener() {
		unregisterPlayerListener(currentMediaSession.player, defaultPlayerLister)
		eventCollectorJob.cancel()
	}

	private fun serviceEventChanged(event: Lifecycle.Event) {
		val get = MusicLibraryService.ServiceEvent.fromLifecycleEvent(event)
		serviceEventChanged(get)
	}

	private fun serviceEventChanged(event: MusicLibraryService.ServiceEvent) {
		Timber.i("EventListener ServiceEvent Changed to $event")

		when (event) {
			MusicLibraryService.ServiceEvent.ON_START_COMMAND -> serviceStateStartCommandImpl()
			MusicLibraryService.ServiceEvent.ON_DESTROY -> serviceStateDestroyedImpl()
			else -> Unit // TODO
		}
	}

	private fun serviceStateStartCommandImpl() {
		service.mainScope.launch { mediaEventHandler.handleServiceStartCommand() }
	}

	private fun serviceStateDestroyedImpl() {
		if (releaseSelf) release()
		service.mainScope.launch { mediaEventHandler.handleServiceRelease() }
	}

	private var playWhenReadyChangedJob = Job().job
	private fun playWhenReadyChangedImpl(session: MediaSession) {
		playWhenReadyChangedJob.cancel()
		playWhenReadyChangedJob = service.mainScope.launch {
			mediaEventHandler.handlePlayerPlayWhenReadyChanged(session)
		}
	}

	private var mediaItemTransitionJob = Job().job
	private fun mediaItemTransitionImpl(session: MediaSession, reason: Int) {
		mediaItemTransitionJob.cancel()
		mediaItemTransitionJob = service.mainScope.launch {
			mediaEventHandler.handlePlayerMediaItemChanged(currentMediaSession, reason)
		}
	}

	private var playerErrorJob = Job().job
	private fun playerErrorImpl(session: MediaSession, error: PlaybackException) {
		playerErrorJob.cancel()
		playerErrorJob = service.mainScope.launch {
			mediaEventHandler.handlePlayerError(session, error)
		}
	}

	private var playerStateChangedJob = Job().job
	private fun playerStateChangedImpl(session: MediaSession, playbackState: @Player.State Int) {
		playerStateChangedJob.cancel()
		playerStateChangedJob = service.mainScope.launch {
			mediaEventHandler.handlePlayerStateChanged(session)
		}
	}

	private var repeatModeChangedJob = Job().job
	private fun repeatModeChangedImpl(session: MediaSession, mode: Int) {
		repeatModeChangedJob.cancel()
		repeatModeChangedJob = service.mainScope.launch {
			mediaEventHandler.handlePlayerRepeatModeChanged(session)
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

	/**
	 * start listening to [MusicLibraryService events]
	 *
	 * @param stopSelf stop itself automatically
	 *
	 * @throws IllegalStateException if not called from [Main Thread]
	 */

	@MainThread
	fun start(stopSelf: Boolean, releaseSelf: Boolean) {
		checkMainThread()

		startDefaultListener()
		if (stopSelf) {
			this.stopSelf = true
		}
		if (releaseSelf) {
			this.releaseSelf = true
		}
	}

	fun stop() {
		stopDefaultListener()
	}

	fun release() {
		checkMainThread()
		if (!state.isReleased()) {
			stopDefaultListener()
			unregisterAllListener(this)
			state = STATE.RELEASED
		}
	}

	fun registerPlayerListener(player: Player, listener: Player.Listener) {
		if (state.isReleased()) {
			Timber.e("EventListener tried to registerPlayerListener on RELEASED state")
			return
		}

		player.addListener(listener)
		this.playerListeners.add(player to listener)
	}

	fun unregisterPlayerListener(player: Player): Boolean {
		return this.playerListeners.removeAll {
			elseFalse(it.first === player) {
				it.first.removeListener(it.second)
			}
		}
	}

	fun unregisterPlayerListener(listener: Player.Listener): Boolean {
		return this.playerListeners.removeAll {
			elseFalse(listener === playerListeners) {
				it.first.removeListener(it.second)
			}
		}
	}

	fun unregisterPlayerListener(player: Player, listener: Player.Listener): Boolean {
		return this.playerListeners.removeAll {
			elseFalse(it.first === player && it.second == listener) {
				it.first.removeListener(it.second)
			}
		}
	}

	fun unregisterAllListener(obj: Any) {
		checkMainThread()
		playerListeners.forEachClear { it.first.removeListener(it.second) }
		Timber.d("$obj made call to unregisterAllListener")
	}

	private inner class LifecycleObserverImpl : LifecycleEventObserver {
		override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
			when (event) {
				ON_CREATE -> serviceEventChanged(MusicLibraryService.ServiceEvent.ON_CREATE)
				ON_START -> launchEventCollector()
				else -> Unit
			}
		}
	}

	private inner class PlayerListenerImpl : Player.Listener {

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			Timber.d("EventListener onPlayWhenReadyChanged")

			super.onPlayWhenReadyChanged(playWhenReady, reason)
			playWhenReadyChangedImpl(currentMediaSession)
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			Timber.d("EventListener onPlaybackStateChanged")

			super.onPlaybackStateChanged(playbackState)
			playerStateChangedImpl(currentMediaSession, playbackState)
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			super.onMediaItemTransition(mediaItem, reason)
			mediaItemTransitionImpl(currentMediaSession, reason)
		}

		override fun onRepeatModeChanged(repeatMode: Int) {
			super.onRepeatModeChanged(repeatMode)
			repeatModeChangedImpl(currentMediaSession, repeatMode)
		}

		override fun onPlayerError(error: PlaybackException) {
			super.onPlayerError(error)
			playerErrorImpl(currentMediaSession, error)
		}
	}

	sealed class STATE {
		object INITIALIZED : STATE()
		object IDLE : STATE()
		object STARTED : STATE()
		object STOPPED : STATE()
		object RELEASED : STATE()

		fun isListening() = this == STARTED
		fun isReleased() = this == RELEASED
	}
}
