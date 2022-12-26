package com.flammky.kotlin.common.lazy

import kotlin.reflect.KProperty

/**
 * Lazy delegate, but with construct function instead of constructor
 */

class LazyConstructor<T> @JvmOverloads constructor(lock: Any = Any()) {

	/**
	 * Placeholder Object
	 */
	private object UNSET

	/**
	 * The Lock
	 */
	private val localLock: Any = lock

	/**
	 * The value holder. [UNSET] if not set
	 *
	 * @throws IllegalStateException if trying to set value without lock
	 * @throws IllegalStateException if value was already set
	 */
	private var localValue: Any? = UNSET
		set(value) {
			check(Thread.holdsLock(localLock)) {
				"Trying to set field without lock"
			}
			check(field === UNSET) {
				"localValue was $field when trying to set $value"
			}
			field = value
		}

	@Suppress("UNCHECKED_CAST")
	private val castValue: T
		get() = try {
			localValue as T
		} catch (cce: ClassCastException) {
			error("localValue=$localValue was UNSET")
		}

	/**
	 * The value.
	 *
	 * @throws IllegalStateException if [localValue] is [UNSET]
	 */
	val value: T
		get() {
			if (!isConstructed()) {
				// The value is not yet initialized, check if its still being initialized.
				// If not then IllegalStateException will be thrown
				sync()
			}
			return castValue
		}

	val lazy = object : Lazy<T> {
		override val value: T get() = this@LazyConstructor.value
		override fun isInitialized(): Boolean = this@LazyConstructor.isConstructed()
	}

	/**
	 *  Whether [localValue] is already initialized
	 *  @see isConstructedAtomic
	 */
	fun isConstructed() = localValue !== UNSET

	/**
	 * Whether [localValue] is already initialized, atomically
	 * @see isConstructed
	 */
	fun isConstructedAtomic() = sync { isConstructed() }

	/** Construct the delegated value, if not already constructed */
	fun construct(lazyValue: () -> T): T {
		if (isConstructed()) {
			return castValue
		}
		return sync {
			if (!isConstructed()) {
				localValue = lazyValue()
			}
			castValue
		}
	}

	fun constructOrThrow(
		lazyValue: () -> T,
		lazyThrow: () -> Nothing
	): T {
		if (isConstructed()) {
			lazyThrow()
		}
		return sync {
			if (!isConstructed()) {
				localValue = lazyValue()
			} else {
				lazyThrow()
			}
			castValue
		}
	}

	private fun sync(): Unit = sync { }
	private fun <T> sync(block: () -> T): T = synchronized(localLock) { block() }

	companion object {
		fun <T> LazyConstructor<T>.valueOrNull(): T? {
			return try { value } catch (ise: IllegalStateException) { null }
		}

		operator fun <T> LazyConstructor<T>.getValue(receiver: Any?, property: KProperty<*>): T {
			return value
		}

		operator fun <T> LazyConstructor<T>.setValue(receiver: Any?, property: KProperty<*>, value: T) {
			construct { value }
		}
	}
}
