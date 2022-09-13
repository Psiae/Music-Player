package com.kylentt.musicplayer.domain.musiclib.annotation


/**
 * denotes that the annotated target is `Experimental` and is prone to change
 */

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.PROPERTY, AnnotationTarget.FUNCTION)
annotation class Experimental()
