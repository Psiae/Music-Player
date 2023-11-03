package dev.dexsr.klio.base.kt

fun <T, R> referentialEqualityFun(): (T, R) -> Boolean = { old, new -> old === new }
