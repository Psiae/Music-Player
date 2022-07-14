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
	fun <R> inject(cls: Class<R>, subclass: Boolean): R? {
		localProviders.forEach {
			if (it.valueClass == cls ) return it.value as R
		}
		if (subclass) {
			localProviders.forEach {
				if (cls.isAssignableFrom(it.valueClass)) return it.value as /* Super */ R }
		}
		return null
	}

	fun fuseInjector(from: Injector) {
		this.addProvider(*from.localProviders.toTypedArray())
	}

	inline fun <reified R: Any> inject(subclass: Boolean): Lazy<R> {
		return LazyValue(R::class.java, subclass)
	}

	inline fun <reified R> lateInject(subclass: Boolean): Lazy<R?> {
		return LateLazyValue(R::class.java, subclass)
	}

	inner class LateLazyValue<T>(
		private val cls: Class<T>,
		private val subclass: Boolean
	): Lazy<T?> {

		private var _value: T? = null

		override val value: T?
			get() {
				if (!isInitialized()) {
					sync { if (!isInitialized()) _value = inject(cls, subclass) }
				}
				return _value
			}

		override fun isInitialized(): Boolean = _value != null
	}

	inner class LazyValue<T: Any>(
		private val cls: Class<T>,
		private val subclass: Boolean
	): Lazy<T> {

		private var _value: T? = null

		override val value: T
			get() {
				if (!isInitialized()) {
					sync { if (!isInitialized()) _value = inject(cls, subclass) }
				}
				return _value!!
			}


		override fun isInitialized(): Boolean = _value != null
	}
}
