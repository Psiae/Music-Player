package com.flammky.android.medialib.player

import kotlinx.coroutines.Deferred

interface AsyncPlayerController : PlayerController, AsyncPlayer {
	fun connectAsync(): Deferred<Boolean>
}
