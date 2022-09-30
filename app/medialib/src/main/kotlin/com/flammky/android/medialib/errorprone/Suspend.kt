package com.flammky.android.medialib.errorprone

/**
 * Denotes that this function should not be called from a suspend function, for [TODO] reason
 */
@Target(AnnotationTarget.FUNCTION)
annotation class UnsafeBySuspend()
