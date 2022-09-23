package com.flammky.android.medialib.temp.api.player

import com.flammky.android.medialib.temp.media3.internal.mediacontroller.ThreadLockedPlayer
import com.flammky.android.medialib.temp.player.LibraryPlayer
import com.google.common.util.concurrent.ListenableFuture

interface MediaController : ThreadLockedPlayer<MediaController> {
	val connected: Boolean
	fun connectService(): ListenableFuture<MediaController>
}

