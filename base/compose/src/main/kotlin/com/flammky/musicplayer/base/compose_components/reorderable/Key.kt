package dev.flammky.compose_components.reorderable

import java.util.Objects

internal class InternalReorderableItemKey(
    val actualKey: Any
) {

    override fun equals(other: Any?): Boolean {
        return other != null && other == actualKey
    }

    override fun hashCode(): Int {
        return Objects.hash(actualKey)
    }

    override fun toString(): String {
        return "InternalReorderableItemKey(actualKey=$actualKey)"
    }
}