package com.flammky.android.medialib.temp.player.event.internal

import androidx.media3.common.MediaItem
import com.flammky.android.medialib.temp.player.event.LibraryPlayerEventListener
import com.flammky.android.medialib.temp.player.event.MediaItemTransitionReason

internal class EventDispatcher() {
	private val eventListeners: MutableList<LibraryPlayerEventListener> = mutableListOf()

	fun onMediaItemTransition(old: MediaItem?, new: MediaItem?, reason: MediaItemTransitionReason) {
		eventListeners.forEach { it.onMediaItemTransition(old, new, reason) }
	}

	fun addListener(listener: LibraryPlayerEventListener) = eventListeners.add(listener)
	fun removeListener(listener: LibraryPlayerEventListener) = eventListeners.removeAll { it === listener }
}
