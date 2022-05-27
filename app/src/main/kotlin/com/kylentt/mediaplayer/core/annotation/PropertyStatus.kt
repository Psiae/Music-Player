package com.kylentt.mediaplayer.core.annotation

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class StableClass

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class StableFunction

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class UnstableClass

@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class UnstableFunction
