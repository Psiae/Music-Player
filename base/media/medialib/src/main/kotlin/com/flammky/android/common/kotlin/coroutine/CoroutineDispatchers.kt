package com.flammky.android.common.kotlin.coroutine

import com.flammky.common.kotlin.coroutine.CoroutineDispatchers

inline val CoroutineDispatchers.ANDROID
    get() = com.flammky.android.kotlin.coroutine.AndroidCoroutineDispatchers.DEFAULT
