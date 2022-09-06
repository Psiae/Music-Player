package com.kylentt.musicplayer.medialib.api.session.component.internal

import com.kylentt.musicplayer.medialib.api.session.component.SessionComponent

class ApiSessionComponent internal constructor(private val internal: InternalSessionComponent) : SessionComponent by internal {
}
