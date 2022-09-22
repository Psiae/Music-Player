package com.flammky.android.medialib.async.player

import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.player.LibraryPlayer
import kotlinx.coroutines.Deferred

// should we really extend `LibraryPlayer`?
interface AsyncPlayer : LibraryPlayer {

	fun prepareAsync(): Deferred<Unit>
	fun playAsync(item: MediaItem): Deferred<Unit>
	fun pauseAsync(): Deferred<Unit>
	fun stopAsync(): Deferred<Unit>
	fun getPlaybackInfoAsync(): Deferred<LibraryPlayer.PlaybackInfo>
	fun getTimelineInfoAsync(): Deferred<LibraryPlayer.TimelineInfo>
}
