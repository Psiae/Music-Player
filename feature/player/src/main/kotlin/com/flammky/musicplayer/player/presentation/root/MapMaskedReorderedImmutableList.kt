package com.flammky.musicplayer.player.presentation.root

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
abstract class MapMaskedReorderedImmutableList<T> {
    abstract fun copyMove(from: Int, to: Int): MapMaskedReorderedImmutableList<T>
    abstract operator fun get(index: Int): T
}

@Immutable
class EmptyMapMaskedReorderedImmutableList<T>(
    val base: ImmutableList<T>
) : MapMaskedReorderedImmutableList<T>() {

    override fun copyMove(from: Int, to: Int): MapMaskedReorderedImmutableList<T> {
        if (from == to) {
            return EmptyMapMaskedReorderedImmutableList(base)
        }
        val newMap = mutableMapOf<Int, Int>()
        newMap[from] = to
        return MapMaskedReorderedImmutableListImpl(
            base,
            newMap,
            minOf(from, to)..maxOf(to, from)
        )
    }

    override fun get(index: Int): T = base[index]
}

@Immutable
internal class MapMaskedReorderedImmutableListImpl<T>(
    private val base: ImmutableList<T>,
    private val maskToIndexMapping: Map<Int, Int>,
    private val maskRange: IntRange
): MapMaskedReorderedImmutableList<T>() {

    override fun copyMove(from: Int, to: Int): MapMaskedReorderedImmutableList<T> {
        if (from == to) {
            return MapMaskedReorderedImmutableListImpl(base, maskToIndexMapping, maskRange)
        }
        val actualFromIndex = maskToIndexMapping[from] ?: from
        val actualToIndex = maskToIndexMapping[to] ?: to
        val newMap = maskToIndexMapping.toMutableMap()
        newMap[from] = actualToIndex
        if (from < to) {
            for (i in from until to) {
                val current = newMap[i + 1]
                val next = newMap[i + 1] ?: (i + 1)
                if (current == next) newMap.remove(current) else newMap[i] = next
            }
        } else {
            for (i in from downTo to + 1) {
                val current = newMap[i - 1]
                val prev = newMap[i - 1] ?: (i - 1)
                if (current == prev) newMap.remove(current) else newMap[i] = prev
            }
        }
        newMap[to] = actualFromIndex
        if (newMap.size == 1) {
            val firstEntry = newMap.entries.first()
            if (firstEntry.key == firstEntry.value) {
                return EmptyMapMaskedReorderedImmutableList(base)
            }
        }
        val newMask =
            minOf(maskRange.first, maskRange.last, from , to)..
                    maxOf(maskRange.first, maskRange.last, from, to)
        return MapMaskedReorderedImmutableListImpl(base, maskToIndexMapping, newMask)
    }

    override fun get(index: Int): T {
       return if (index in maskRange) {
           base[maskToIndexMapping[index]!!]
       } else {
           base[index]
       }
    }
}