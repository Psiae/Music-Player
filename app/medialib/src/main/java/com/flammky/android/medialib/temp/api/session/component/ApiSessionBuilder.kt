package com.flammky.android.medialib.temp.api.session.component

import com.flammky.android.medialib.temp.api.session.component.internal.InternalSessionBuilder

class ApiSessionBuilder internal constructor(private val internal: InternalSessionBuilder) : SessionBuilder by internal {

}
