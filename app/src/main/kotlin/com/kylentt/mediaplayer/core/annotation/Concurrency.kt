package com.kylentt.mediaplayer.core.annotation

/**
 * Target is safe to call from any Thread
 */
@Retention(AnnotationRetention.BINARY)
@Target(
	AnnotationTarget.CLASS,
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.PROPERTY_SETTER
)
annotation class ThreadSafe
