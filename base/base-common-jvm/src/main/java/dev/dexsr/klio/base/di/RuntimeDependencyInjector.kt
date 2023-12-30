package dev.dexsr.klio.base.di

import kotlin.reflect.KClass

interface RuntimeDependencyInjector {

    fun <T: Any> requireInject(clazz: KClass<T>): T
}

inline fun <reified T: Any> RuntimeDependencyInjector.requireInject(): T = requireInject(T::class)
