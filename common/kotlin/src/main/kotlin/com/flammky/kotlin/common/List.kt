package com.flammky.kotlin.common

fun <E> List<E>.subListAroundIndex(
	index: Int,
	range: Int,
): List<E> = subList(index - range, index + range + 1)

fun <E> List<E>.subListSpreadFromIndex(
	index: Int,
	range: Int,
	ignoreBounds: Boolean = false
): List<E> {
	require(index >= 0 && range >= 0 && index in indices) {
		"Invalid Argument, index=$index range=$range index=$index indices$indices"
	}
	if (range == 0) {
		return listOf(get(index))
	}
	val rangeBound = minOf(index, lastIndex - index)
	val iteratorRange = if (ignoreBounds) {
		minOf(range, rangeBound)
	} else {
		require(range <= rangeBound) {
			"Invalid Argument, range=$range is out of bounds=$rangeBound"
		}
		range
	}

	/*for (i in 0.. iteratorRange * 2) {
		val element = if (i % 2 == 0) {
			get(index + i / 2)
		} else {
			get(index - (i / 2 + 1))
		}
		result.add(element)
	}*/

	val result = mutableListOf<E>()
	var direction = 1
	var currentIndex = index
	var nextStep = 1
	while (result.size < iteratorRange * 2 + 1) {
		result.add(get(currentIndex))
		direction *= -1
		currentIndex += direction * nextStep++
	}
	return result
}
