package com.flammky.android.medialib.temp.annotation

import androidx.annotation.IntRange

/**
 * Denotes that the annotated target should only be called on the given API level
 */

@Target(
	AnnotationTarget.CLASS,
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.PROPERTY_SETTER,
	AnnotationTarget.CONSTRUCTOR,
	AnnotationTarget.FIELD,
	AnnotationTarget.FILE
)
annotation class OnlyAPI(
	@IntRange(from = 1) val level: Int
)
