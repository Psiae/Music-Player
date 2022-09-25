package com.flammky.android.medialib.player

/**
 * Instance that delegates call to the controlled Player,
 *
 * kind of similar to MediaController and ForwardingPlayer
 */
interface PlayerController : Player {
	fun connect(): Boolean
}
