package dev.dexsr.klio.base.util

import java.util.*

class KeyEqualsValue <K, V>(val key: K, val value: V) {

    override fun equals(other: Any?): Boolean {
        return other is KeyEqualsValue<*, *> && other.key == key
    }

    override fun hashCode(): Int = Objects.hash(key)

    override fun toString(): String = "KeyEqualsValue($key, $value)"
}
