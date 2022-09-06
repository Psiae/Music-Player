package com.kylentt.musicplayer.medialib.api.session.component

import com.kylentt.musicplayer.medialib.api.session.component.internal.InternalSessionManager
import com.kylentt.musicplayer.medialib.session.LibrarySession

class ApiSessionManager internal constructor(private val internal: InternalSessionManager) : SessionManager by internal {
}
