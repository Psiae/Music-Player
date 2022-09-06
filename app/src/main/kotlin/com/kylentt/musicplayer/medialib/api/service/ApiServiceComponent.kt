package com.kylentt.musicplayer.medialib.api.service

import com.kylentt.musicplayer.medialib.api.service.internal.InternalServiceComponent

class ApiServiceComponent internal constructor(private val internal: InternalServiceComponent) : ServiceComponent by internal {

}
