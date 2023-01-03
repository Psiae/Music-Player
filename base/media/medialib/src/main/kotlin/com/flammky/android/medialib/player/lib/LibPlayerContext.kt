package com.flammky.android.medialib.player.lib

import com.flammky.android.medialib.context.LibraryContext
import com.flammky.android.medialib.player.PlayerContext

abstract class LibPlayerContext internal constructor() : PlayerContext() {
	abstract val library: LibraryContext
}
