package com.flammky.common.kotlin.lazy

import androidx.annotation.GuardedBy

/**
 * Lazy delegate, but with construct function instead of constructor
 */

class LazyConstructor<T> @JvmOverloads constructor(lock: Any = Any()) : Lazy<T> {

	/**
	 * Placeholder Object
	 */
	private object UNSET

	/** The Lock */
	private val localLock: Any = lock

	/** The value holder. [UNSET] if not set  */
	private var localValue: Any? = UNSET
		@GuardedBy("localLock")
		@kotlin.jvm.Throws(IllegalStateException::class)
		set(value) {
			check(Thread.holdsLock(localLock)) {
				"Trying to set guarded field without lock"
			}
			check(field === UNSET) {
				"Lazy Constructor failed, localValue was $field when trying to set $value"
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
	override val value: T
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

	/**
	 * Construct the delegated value, if not already constructed
	 */
	fun construct(lazyValue: () -> T): T {
		if (!isConstructed()) sync {
			// check again if value is already initialized by the time it holds the lock
			if (!isConstructed()) {
				// was not initialized, should be safe to invoke
				localValue = lazyValue()
			}
		}
		return castValue
	}

	override fun isInitialized(): Boolean = isConstructedAtomic()

	private fun sync(): Unit = sync { }
	private fun <T> sync(block: () -> T): T = synchronized(localLock) { block() }
}
