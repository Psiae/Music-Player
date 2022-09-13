package com.flammky.android.medialib.temp.api.session.component.internal

import com.flammky.android.medialib.temp.api.session.component.SessionComponent

class ApiSessionComponent internal constructor(private val internal: InternalSessionComponent) : SessionComponent by internal {

}
