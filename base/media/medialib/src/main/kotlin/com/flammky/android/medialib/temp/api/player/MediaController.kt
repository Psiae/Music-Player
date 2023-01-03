package com.flammky.android.medialib.temp.api.player

import com.flammky.android.medialib.common.mediaitem.MediaItem
import com.flammky.android.medialib.temp.media3.internal.mediacontroller.ThreadLockedPlayer
import com.google.common.util.concurrent.ListenableFuture

interface MediaController : ThreadLockedPlayer<MediaController> {
	val currentActualMediaItem: MediaItem?

	val connected: Boolean
	fun connectService(): ListenableFuture<MediaController>
}

