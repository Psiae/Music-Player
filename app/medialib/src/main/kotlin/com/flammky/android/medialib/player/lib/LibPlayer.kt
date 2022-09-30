package com.flammky.android.medialib.player.lib

import com.flammky.android.medialib.player.Player

abstract class LibPlayer : Player {
	protected abstract val context: LibPlayerContext
}
