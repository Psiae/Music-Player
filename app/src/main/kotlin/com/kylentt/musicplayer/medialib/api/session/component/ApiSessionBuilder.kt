package com.kylentt.musicplayer.medialib.api.session.component

import com.kylentt.musicplayer.medialib.api.session.component.internal.InternalSessionBuilder

class ApiSessionBuilder internal constructor(private val internal: InternalSessionBuilder) : SessionBuilder by internal {

}
