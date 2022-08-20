package com.kylentt.musicplayer.domain.musiclib.annotation

import java.util.concurrent.TimeUnit

/**
 * Denotes that the value is representing a certain TimeUnit
 */
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.PROPERTY_GETTER)
annotation class TimeUnitValue(
	val unit: TimeUnit
)
