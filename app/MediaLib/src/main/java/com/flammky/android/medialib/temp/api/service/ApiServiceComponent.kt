package com.flammky.android.medialib.temp.api.service

import com.flammky.android.medialib.temp.api.service.internal.InternalServiceComponent

class ApiServiceComponent internal constructor(private val internal: InternalServiceComponent) : ServiceComponent by internal {

}
