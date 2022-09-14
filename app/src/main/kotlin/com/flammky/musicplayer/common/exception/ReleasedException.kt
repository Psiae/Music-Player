package com.flammky.musicplayer.common.exception

/**
 *
 * Signals that a method has been invoked at an illegal or
 * inappropriate time.  In other words, the Java environment or
 * Java application is not in an appropriate state for the requested
 * operation.
 *
 * ++ A method is invoked at a `Released State` and the requested operation is not possible
 */

open class ReleasedException : IllegalStateException {

	constructor() : super()

	constructor(message: String) : super(message)

	constructor(cause: Throwable) : super(cause)

	constructor(message: String, cause: Throwable) : super(message, cause)


	companion object {
		const val serialVersionUID = 131L
	}
}
