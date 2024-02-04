package dev.dexsr.klio.base.concurrency.annotations

/**
 * Denotes that the annotated method may not be called concurrently.
 * If the annotated element is a class, then this applies to all methods in the class.
**/
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.PROPERTY_SETTER,
	AnnotationTarget.CONSTRUCTOR,
	AnnotationTarget.ANNOTATION_CLASS,
	AnnotationTarget.CLASS,
	AnnotationTarget.VALUE_PARAMETER
)
annotation class NoConcurrentCall
