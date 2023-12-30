package dev.dexsr.klio.base.kt

import kotlin.time.DurationUnit

val DurationUnit.isSecond: Boolean get() = this == DurationUnit.SECONDS
