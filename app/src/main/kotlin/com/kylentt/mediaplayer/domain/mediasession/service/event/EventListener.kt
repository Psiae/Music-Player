package com.kylentt.mediaplayer.domain.mediasession.service.event

import androidx.annotation.MainThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import com.kylentt.mediaplayer.core.coroutines.AppDispatchers
import com.kylentt.mediaplayer.core.coroutines.safeCollect
import com.kylentt.mediaplayer.core.delegates.AutoCancelJob
import com.kylentt.mediaplayer.domain.mediasession.service.MusicLibraryService
import com.kylentt.mediaplayer.domain.mediasession.service.OnChanged
import com.kylentt.mediaplayer.helper.Preconditions.checkMainThread
import com.kylentt.mediaplayer.helper.Preconditions.checkState
import com.kylentt.mediaplayer.ui.activity.CollectionExtension.forEachClear
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class MusicLibraryServiceListener(
	private val manager: MusicLibraryEventManager,
	private val mediaEventHandler: MusicLibraryEventHandler
) {

	private var coroutineDispatchers: AppDispatchers
	private var immediateScope: CoroutineScope
	private var mainScope: CoroutineScope

	private val playerListenerImpl = PlayerListenerImpl()
	private val onPlayerChangedImpl = OnPlayerChangedImpl()

	private val playerListeners = mutableListOf<Pair<Player, Player.Listener>>()

	private var state: STATE = STATE.NOTHING
	private var serviceLastEvent: MusicLibraryService.ServiceEvent? = null

	private var stopSelf: Boolean = false
	private var releaseSelf: Boolean = false

	private var isPlayerChangeRegistered = false

	private var eventCollectorJob by AutoCancelJob()

	val mediaSession: MediaSession
		get() = manager.mediaSession

	val isListening: Boolean
		get() = state.isListening()

	val isReleased: Boolean
		get() = state.isReleased()

	init {
		onInternalEvent(EVENT.INITIALIZING)

		coroutineDispatchers = manager.coroutineDispatchers
		immediateScope = manager.mainImmediateScope
		mainScope = manager.mainScope

		onInternalEvent(EVENT.INITIALIZED)
	}

	@MainThread
	private fun startImpl(stopSelf: Boolean, releaseSelf: Boolean) {
		checkMainThread()

		if (isListening) {
			return Timber.w("EventListener start called when already started")
		}

		onInternalEvent(EVENT.STARTING)

		if (stopSelf) {
			this.stopSelf = true
		}

		if (releaseSelf) {
			this.releaseSelf = true
		}

		startDefaultListener()

		onInternalEvent(EVENT.STARTED)
	}

	@MainThread
	private fun stopImpl() {

		if (!isListening) {
			return Timber.w("EventListener stop called when already stopped")
		}

		onInternalEvent(EVENT.STOPPING)

		checkMainThread()

		stopDefaultListener()

		onInternalEvent(EVENT.STOPPED)
	}

	private fun releaseImpl(obj: Any) {

		if (isReleased) {
			return Timber.w("EventListener release called when already released")
		}

		onInternalEvent(EVENT.RELEASING(obj))

		checkMainThread()

		if (isListening) {
			stopImpl()
		}

		unregisterAllListener(this)

		manager.unregisterPlayerChangedListener(onPlayerChangedImpl)

		onInternalEvent(EVENT.RELEASED(obj))
	}

	private fun startDefaultListener() {

		launchEventCollector()

		if (!isPlayerChangeRegistered) {
			manager.registerPlayerChangedListener(onPlayerChangedImpl)
			isPlayerChangeRegistered = true
		}
	}

	private fun stopDefaultListener() {
		unregisterPlayerListener(mediaSession.player, playerListenerImpl)
		stopEventCollector()
	}


	private fun launchEventCollector() {
		eventCollectorJob = immediateScope.launch {
			manager.serviceEventSF.safeCollect { if (it != serviceLastEvent) serviceEventChanged(it) }
		}
	}

	private fun stopEventCollector() {
		eventCollectorJob.cancel()
	}

	private fun onInternalEvent(event: EVENT) {
		Timber.i("EventListener onInternalEvent $event")

		if (event is STATE.ToState) updateState(event.toState())
	}

	private fun updateState(state: STATE) {
		checkState(!isStateJump(state)) {
			"State Jump from ${this.state} to $state"
		}

		this.state = state
	}

	private fun isStateJump(state: STATE) = !(this.state upFrom state || this.state downFrom state)

	private fun serviceEventChanged(event: MusicLibraryService.ServiceEvent) {
		checkState(this.serviceLastEvent != event) {
			"Tried to update on same ServiceEvent"
		}

		Timber.i("EventListener ServiceEvent Changed to $event")

		when (event) {
			MusicLibraryService.ServiceEvent.ON_START_COMMAND -> serviceStateStartCommandImpl()
			MusicLibraryService.ServiceEvent.ON_DESTROY -> serviceStateDestroyedImpl()
			else -> Unit // TODO
		}

		this.serviceLastEvent = event
	}

	private fun serviceStateStartCommandImpl() {
		immediateScope.launch { mediaEventHandler.handleServiceStartCommand() }
	}

	private fun serviceStateDestroyedImpl() {
		if (releaseSelf) release(this)
		immediateScope.launch { mediaEventHandler.handleServiceRelease() }
	}

	private var playWhenReadyChangedJob by AutoCancelJob()
	private fun playWhenReadyChangedImpl(session: MediaSession) {
		playWhenReadyChangedJob = mainScope.launch {
			mediaEventHandler.handlePlayerPlayWhenReadyChanged(session)
		}
	}

	private var mediaItemTransitionJob by AutoCancelJob()
	private fun mediaItemTransitionImpl(session: MediaSession, reason: Int) {
		mediaItemTransitionJob = mainScope.launch {
			mediaEventHandler.handlePlayerMediaItemChanged(mediaSession, reason)
		}
	}

	private var playerErrorJob by AutoCancelJob()
	private fun playerErrorImpl(session: MediaSession, error: PlaybackException) {
		playerErrorJob = mainScope.launch {
			mediaEventHandler.handlePlayerError(session, error)
		}
	}

	private var playerStateChangedJob by AutoCancelJob()
	private fun playerStateChangedImpl(session: MediaSession, playbackState: @Player.State Int) {
		playerStateChangedJob = mainScope.launch {
			mediaEventHandler.handlePlayerStateChanged(session)
		}
	}

	private var repeatModeChangedJob by AutoCancelJob()
	private fun repeatModeChangedImpl(session: MediaSession, mode: Int) {
		repeatModeChangedJob = mainScope.launch {
			mediaEventHandler.handlePlayerRepeatModeChanged(session)
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

	/**
	 * start listening to [MusicLibraryService] events
	 *
	 * @param stopSelf stop itself
	 * @param releaseSelf release itself
	 *
	 * @throws IllegalStateException if not called from [Main Thread]
	 */

	@MainThread fun start(stopSelf: Boolean, releaseSelf: Boolean) {
		if (!isListening && !isReleased) {
			startImpl(stopSelf, releaseSelf)
		}
	}

	/**
	 * stop listening to [MusicLibraryService] events
	 *
	 * @throws IllegalStateException if not called from [Main Thread]
	 */

	@MainThread fun stop() {
		if (isListening && !isReleased) {
			stopImpl()
		}
	}

	/**
	 * release this listener
	 *
	 * @param obj the object that call this function
	 * @throws IllegalStateException if not called from [Main Thread]
	 */

	@MainThread fun release(obj: Any) {
		if (!isReleased) {
			releaseImpl(obj)
		}
	}

	fun registerPlayerListener(player: Player, listener: Player.Listener) {
		if (isReleased) {
			Timber.e("EventListener tried to registerPlayerListener on RELEASED state")
			return
		}

		player.addListener(listener)
		this.playerListeners.add(player to listener)

		Timber.i("EventListener registered $player to $listener")
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
				Timber.i("EventListener unRegistered $listener from $player")
			}
		}
	}

	fun unregisterAllListener(obj: Any) {
		checkMainThread()
		playerListeners.forEachClear { it.first.removeListener(it.second) }
		Timber.d("$obj made call to unregisterAllListener")
	}

	private inner class PlayerListenerImpl : Player.Listener {

		override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
			Timber.d("EventListener onPlayWhenReadyChanged")

			super.onPlayWhenReadyChanged(playWhenReady, reason)
			playWhenReadyChangedImpl(mediaSession)
		}

		override fun onPlaybackStateChanged(playbackState: Int) {
			Timber.d("EventListener onPlaybackStateChanged")

			super.onPlaybackStateChanged(playbackState)
			playerStateChangedImpl(mediaSession, playbackState)
		}

		override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
			super.onMediaItemTransition(mediaItem, reason)
			mediaItemTransitionImpl(mediaSession, reason)
		}

		override fun onRepeatModeChanged(repeatMode: Int) {
			super.onRepeatModeChanged(repeatMode)
			repeatModeChangedImpl(mediaSession, repeatMode)
		}

		override fun onPlayerError(error: PlaybackException) {
			super.onPlayerError(error)
			playerErrorImpl(mediaSession, error)
		}
	}

	private inner class OnPlayerChangedImpl : OnChanged<Player> {

		override fun onChanged(old: Player?, new: Player) {

			Timber.i("EventListener onPlayerChanged $old to $new")

			val isChanged = old !== new

			checkState(isChanged) {
				"received onPlayerChanged callback when there is no change"
			}

			if (old == null) {
				return registerPlayerListener(new, playerListenerImpl)
			}

			val rem = unregisterPlayerListener(old, playerListenerImpl)

			checkState(rem) {
				"playerListenerImpl was not registered for $old, possible inconsistency"
			}

			Timber.i("EventListener onPlayerChanged was registered for $old, unRegistered")

			val rem2 = playerListeners.find { it.second === playerListenerImpl} == null

			checkState(rem2) {
				"playerListenerImpl was not unRegistered, possible inconsistency" +
					"\n${playerListeners}"
			}

			Timber.i("EventListener onPlayerChanged no $playerListenerImpl registered, registering for $new")

			registerPlayerListener(new, playerListenerImpl)
		}
	}

	private sealed class STATE {

		interface ToState {
			fun toState(): STATE
		}

		object NOTHING : STATE()
		object INITIALIZED : STATE()
		object STARTED : STATE()
		object STOPPED : STATE()

		data class RELEASED(val obj: Any) : STATE()

		fun isListening() = this == STARTED
		fun isReleased() = this is RELEASED

		infix fun upFrom(that: STATE) = INT.get(this) == INT.get(that) + 1
		infix fun downFrom(that: STATE) = INT.get(this) == INT.get(that) -1

		object INT {
			private const val NOTHING = -1
			private const val INITIALIZED = 0
			private const val STARTED = 1
			private const val STOPPED = 0
			private const val RELEASED = -1

			fun get(state: STATE): Int {
				return when (state) {
					STATE.NOTHING -> NOTHING
					STATE.INITIALIZED -> INITIALIZED
					STATE.STARTED -> STARTED
					STATE.STOPPED -> STOPPED
					is RELEASED -> RELEASED
				}
			}
		}
	}

	private sealed class EVENT {
		object INITIALIZING : EVENT()

		object INITIALIZED : EVENT(), STATE.ToState {
			override fun toState(): STATE = STATE.INITIALIZED
		}

		object STARTING : EVENT()

		object STARTED : EVENT(), STATE.ToState {
			override fun toState(): STATE = STATE.STARTED
		}

		object STOPPING : EVENT()

		object STOPPED : EVENT(), STATE.ToState {
			override fun toState(): STATE = STATE.STOPPED
		}

		data class RELEASING(val obj: Any) : EVENT()

		data class RELEASED(val obj: Any) : EVENT(), STATE.ToState {
			override fun toState(): STATE = STATE.RELEASED(obj)
		}
	}
}
