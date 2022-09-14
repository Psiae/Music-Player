package com.flammky.musicplayer.common.android.exception

import com.flammky.android.common.io.exception.PermissionException

class ReadStoragePermissionException : PermissionException {
	constructor() : super()

	constructor(message: String?) : super(message)

	constructor(message: String?, cause: Throwable?) : super(message, cause)

	constructor(cause: Throwable?) : super(cause)
}
