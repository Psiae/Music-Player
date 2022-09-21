package com.flammky.android.common.io.exception

import java.io.IOException

open class PermissionException : IOException {
	constructor() : super()

	constructor(message: String?) : super(message)

	constructor(message: String?, cause: Throwable?) : super(message, cause)

	constructor(cause: Throwable?) : super(cause)
}
