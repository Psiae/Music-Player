package com.flammky.android.medialib.temp.media3.internal.mediacontroller

import android.os.Looper
import com.flammky.android.medialib.concurrent.PublicThreadLocked
import com.flammky.android.medialib.temp.player.LibraryPlayer

interface ThreadLockedPlayer<P: LibraryPlayer> : LibraryPlayer, PublicThreadLocked<P> {
	val publicLooper: Looper
}
