package com.flammky.common.kotlin.lazy

/**
 * Lazy delegate, but with construct function instead of constructor
 */

class LazyConstructor<T> @JvmOverloads constructor(lock: Any = Any()) : Lazy<T> {

	/**
	 * Placeholder Object, delegated value may be null
	 */
	private object UNSET

	/** The Lock */
	private val localLock: Any = lock

	/** The value holder. [UNSET] if not initialized  */
	private var localValue: Any? = UNSET
		set(value) {
			require(field === UNSET && Thread.holdsLock(localLock)) {
				"Lazy Constructor failed, localValue was $field when trying to set $value"
			}
			field = value
		}

	/**
	 *  Whether [localValue] is already initialized
	 *  @see isConstructedAtomic
	 */

	fun isConstructed() = localValue !== UNSET

	/**
	 * Whether [localValue] is already initialized, atomically
	 */

	fun isConstructedAtomic() = sync { isConstructed() }

	override fun isInitialized(): Boolean = isConstructedAtomic()

	/**
	 * The value.
	 *
	 * @throws ClassCastException if [localValue] is [UNSET]
	 */
	override val value: T
		@Throws(ClassCastException::class)
		get() {
			if (!isConstructed()) {
				// The value is not yet initialized, check if its still being initialized.
				// If not then ClassCastException will be thrown
				sync()
			}
			@Suppress("UNCHECKED_CAST")
			return localValue as T
		}

	/**
	 * Try to get the value.
	 *
	 * Null if not yet initialized
	 */
	val valueOrNull: T?
		get() = try { value } catch (e: ClassCastException) { null }

	fun construct(lazyValue: () -> T): T {
		if (!isConstructed()) sync {
			// check again if value is already initialized by the time it holds the lock
			if (!isConstructed()) {
				// was not initialized, should be safe to invoke
				localValue = lazyValue()
			}
		}
		return value
	}

	private fun sync(): Unit = sync { }
	private fun <T> sync(block: () -> T): T = synchronized(localLock) { block() }
}
