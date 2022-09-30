package com.flammky.kotlin.common.lazy

/**
 * Lazy delegate, but with construct function instead of constructor
 */

class LazyConstructor<T> @JvmOverloads constructor(lock: Any = Any()) {

	/** Placeholder Object */
	private object UNSET

	/** The Lock */
	private val localLock: Any = lock

	/** The value holder. [UNSET] if not set  */
	private var localValue: Any? = UNSET
		@kotlin.jvm.Throws(IllegalStateException::class)
		set(value) {
			check(Thread.holdsLock(localLock)) {
				"Trying to set field without lock"
			}
			check(field === UNSET) {
				"localValue was $field when trying to set $value"
			}
			field = value
		}

	private val castValue: T
		@kotlin.jvm.Throws(IllegalStateException::class)
		get() = try {
			@Suppress("UNCHECKED_CAST")
			localValue as T
		} catch (cce: ClassCastException) {
			error("localValue($localValue) was UNSET")
		}

	/**
	 * The value.
	 *
	 * @throws IllegalStateException if [localValue] is [UNSET]
	 */
	val value: T
		@kotlin.jvm.Throws(IllegalStateException::class)
		get() {
			if (!isConstructed()) {
				// The value is not yet initialized, check if its still being initialized.
				// If not then IllegalStateException will be thrown
				sync()
			}
			return castValue
		}

	/**
	 * Try to get the value.
	 *
	 * Null if not yet initialized
	 */
	val valueOrNull: T?
		get() = if (isConstructed()) {
			castValue
		} else {
			null
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
		if (!isConstructed()) sync {
			if (!isConstructed()) {
				localValue = lazyValue()
			}
		}
		return castValue
	}

	private fun sync(): Unit = sync { }
	private fun <T> sync(block: () -> T): T = synchronized(localLock) { block() }
}
