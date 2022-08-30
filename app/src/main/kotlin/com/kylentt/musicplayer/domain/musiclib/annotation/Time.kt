package com.kylentt.musicplayer.domain.musiclib.annotation

import java.util.concurrent.TimeUnit

/**
 * Denotes that the value is a Duration, in certain [TimeUnit]
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class DurationValue(
	val unit: TimeUnit
)
