package com.kylentt.musicplayer.common.android.exception

import com.flammky.android.common.io.exception.PermissionException

class WriteStoragePermissionException : PermissionException {
	constructor() : super()

	constructor(message: String?) : super(message)

	constructor(message: String?, cause: Throwable?) : super(message, cause)

	constructor(cause: Throwable?) : super(cause)
}
