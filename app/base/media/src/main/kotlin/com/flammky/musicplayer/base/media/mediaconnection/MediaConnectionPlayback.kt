package com.flammky.musicplayer.base.media.mediaconnection

import android.net.Uri
import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.temp.media3.Timeline
import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration

interface MediaConnectionPlayback {
	var playWhenReady: Boolean
	val playing: Boolean
	val index: Int

	fun prepare()
	fun stop()

	fun bufferedPosition(): Duration
	fun position(): Duration
	fun duration(): Duration

	fun setMediaItems(items: List<MediaItem>, startIndex: Int, startPosition: Duration)


	fun getCurrentMediaItem(): MediaItem?
	fun getPlaylist(): List<MediaItem>

	fun play(mediaItem: MediaItem)
	fun pause()

	fun observeMediaItem(): Flow<MediaItem?>
	fun observePlaylist(): Flow<List<String>>
	fun observePosition(): Flow<Duration>
	fun observeDuration(): Flow<Duration>
	fun observeIsPlaying(): Flow<Boolean>
	fun observePlayWhenReady(): Flow<Boolean>
	fun observeTimeline(): Flow<Timeline>
	fun observeDiscontinuity(): Flow<Duration>

	suspend fun <R> joinDispatcher(block: suspend MediaConnectionPlayback.() -> R): R

	fun notifyUnplayableMedia(id: String)
	fun notifyUnplayableMedia(uri: Uri)
}
