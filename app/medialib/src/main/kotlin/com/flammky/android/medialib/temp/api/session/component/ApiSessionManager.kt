package com.flammky.android.medialib.temp.api.session.component

import com.flammky.android.medialib.temp.api.session.component.internal.InternalSessionManager

class ApiSessionManager internal constructor(private val internal: InternalSessionManager) : SessionManager by internal {
}
