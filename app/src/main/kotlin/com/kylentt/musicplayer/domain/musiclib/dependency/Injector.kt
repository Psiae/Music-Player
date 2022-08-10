package com.kylentt.musicplayer.domain.musiclib.dependency

import com.kylentt.musicplayer.common.generic.sync

class Injector {
	private val mParents: MutableSet<Injector> = mutableSetOf()
	private val localProviders: MutableList<Provider<Any>> = mutableListOf()

	fun addProvider(vararg providers: Provider<Any>): Unit = localProviders.sync {
		providers.forEach { provider ->
			find { local -> local.valueClass == provider.valueClass } ?: add(provider)
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun <R> inject(cls: Class<R>, subclass: Boolean): R? {
		localProviders.forEach { provider ->
			if (provider.valueClass == cls) return provider.value as R
		}

		if (subclass) {
			localProviders.forEach { provider ->
				if (cls.isAssignableFrom(provider.valueClass)) return provider.value as /* Super */ R
			}
		}

		mParents.forEach { parent ->
			parent.inject(cls, subclass)?.let { fromParent -> return fromParent }
		}

		return null
	}

	fun fuse(injector: Injector): Unit {
		mParents.add(injector)
	}

	inline fun <reified R : Any> inject(subclass: Boolean = false): Lazy<R> {
		return LazyValue(R::class.java, subclass)
	}

	inline fun <reified R> lateInject(subclass: Boolean = false): Lazy<R?> {
		return LateLazyValue(R::class.java, subclass)
	}

	inner class LateLazyValue<T>(
		private val cls: Class<T>,
		private val subclass: Boolean
	) : Lazy<T?> {

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

	inner class LazyValue<T : Any>(
		private val cls: Class<T>,
		private val subclass: Boolean
	) : Lazy<T> {

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
