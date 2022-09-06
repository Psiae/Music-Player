package com.kylentt.musicplayer.common.lazy

/**
 * Lazy delegate, but with construct function instead of constructor
 */

class LazyConstructor<T> @JvmOverloads constructor(lock: Any = Any()) : Lazy<T> {

	/** Placeholder Object, delegated value may be null */
	private object EMPTY

	/** The Lock */
	private val localLock: Any = lock

	/** The value holder. [EMPTY] if not initialized  */
	@Volatile private var localValue: Any? = EMPTY
		set(value) {
			require(field === EMPTY) {
				"Latelazy failed, localValue was $field when trying to set $value"
			}
			field = value
		}

	/** Whether [localValue] is already initialized */

	fun isConstructed() = localValue !== EMPTY

	fun isConstructedAtomic() = sync { isConstructed() }

	override fun isInitialized(): Boolean = isConstructed()

	/**
	 * The value.
	 *
	 * @throws ClassCastException if [localValue] is [EMPTY]
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
			// check again if value is already initialized by the time it enters the lock
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
