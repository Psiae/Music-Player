package dev.dexsr.klio.base.util

abstract class ValueContainer<T> internal constructor() {
    abstract val value: T
}

open class ImmutableValueContainer <T> internal constructor(initialValue: T) : ValueContainer<T>() {
    override val value: T = initialValue
}

open class MutableValueContainer <T> internal constructor(initialValue: T): ValueContainer<T>() {
    override var value: T = initialValue
}

class SynchronizedMutableValueContainer<T>(initialValue: T) : MutableValueContainer<T>(initialValue) {
    @get:Synchronized
    @set:Synchronized
    override var value: T
        get() = super.value
        set(value) { super.value = value }
}

public fun <T> immutableValueContainerOf(
    value: T
): ImmutableValueContainer<T> = ImmutableValueContainer(value)
public fun <T> mutableValueContainerOf(
    value: T,
    synchronized: Boolean = false
): MutableValueContainer<T> {
    return if (synchronized)
        SynchronizedMutableValueContainer(value)
    else
        MutableValueContainer(value)
}
