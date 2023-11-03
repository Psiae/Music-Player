package com.flammky.musicplayer.core.common

import java.util.concurrent.atomic.AtomicInteger

@Deprecated("deprecated, use kotlinx.atomicfu.atomic instead",
    ReplaceWith("atomic(v)", "kotlinx.atomicfu.atomic")
)
fun atomic(v: Int): AtomicInteger = AtomicInteger(v)
