package com.flammky.android.medialib.temp.api.player

import androidx.media3.common.MediaItem
import com.google.common.util.concurrent.ListenableFuture
import com.flammky.android.medialib.temp.player.LibraryPlayer

interface MediaController : LibraryPlayer {
	val connected: Boolean
	fun connectService(): ListenableFuture<MediaController>
}

