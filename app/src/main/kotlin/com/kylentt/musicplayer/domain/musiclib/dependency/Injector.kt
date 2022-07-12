package com.kylentt.musicplayer.domain.musiclib.dependency

import com.kylentt.mediaplayer.core.extenstions.sync

class Injector {
	private val localProviders: MutableList<Provider<Any>> = mutableListOf()

	fun addProvider(vararg providers: Provider<Any>): Unit = localProviders.sync {
		providers.forEach { provider ->
			find { local -> local.valueClass == provider.valueClass } ?: add(provider)
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun <R : Any> get(cls: Class<R>, subclass: Boolean): R? {
		localProviders.forEach { if (it.valueClass == cls ) return it.value as R }
		if (subclass) {
			localProviders.forEach { if (cls.isAssignableFrom(it.valueClass)) return it.value as /* Super */ R }
		}
		return null
	}

	fun fuseProvider(from: Injector) {
		from.localProviders.forEach { provider -> this.addProvider(provider) }
	}
}

interface Provider<T: Any> {
	val value: T
	val valueClass: Class<T>
}

class ValueProvider <T: Any>(override val value: T) : Provider<T> {
	override val valueClass: Class<T> = value.javaClass

	companion object {
		fun args(vararg args : Any): Array<ValueProvider<Any>> {
			val mtb = mutableListOf<ValueProvider<Any>>()
			args.forEach { mtb.add(ValueProvider(it)) }
			return mtb.toTypedArray()
		}
	}
}
