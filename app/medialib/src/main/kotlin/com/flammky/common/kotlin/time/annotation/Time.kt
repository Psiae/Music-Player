package com.flammky.common.kotlin.time.annotation

import java.util.concurrent.TimeUnit

/**
 * Denotes that the value is a Duration, in certain [TimeUnit]
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FUNCTION)
annotation class DurationValue(
	val unit: TimeUnit
)
