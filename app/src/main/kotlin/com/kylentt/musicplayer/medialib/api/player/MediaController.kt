package com.kylentt.musicplayer.medialib.api.player

import com.google.common.util.concurrent.ListenableFuture
import com.kylentt.musicplayer.medialib.player.LibraryPlayer

interface MediaController : LibraryPlayer {
	val connected: Boolean
	fun connect(): ListenableFuture<MediaController>
}

