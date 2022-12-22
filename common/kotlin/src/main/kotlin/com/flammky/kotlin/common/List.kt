package com.flammky.kotlin.common

/**
 * sublist around the index within the given range
 * input: listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).subListAroundIndex(4, 2)
 * output: [3, 4, 5, 6, 7]
 * input: listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).subListAroundIndex(1, 2)
 * output: [1, 2, 3, 4]
 *
 * @param index the range point
 * @param range the range
 * @param ignoreBounds whether to ignore any specific bounds constraint
 */
fun <E> List<E>.subListAroundIndex(
	index: Int,
	range: Int,
	ignoreBounds: Boolean = false
): List<E> {
	require(index >= 0 && range >= 0 && index in indices) {
		"Invalid Argument, index=$index range=$range index=$index indices=$indices"
	}
	if (range == 0) {
		return listOf(get(index))
	}
	// should Illegal Argument be thrown here ?
	return if (ignoreBounds) {
		val startBound = index
		val endBound = lastIndex - index
		val startExtra = minOf(startBound, range)
		val endExtra = minOf(endBound, range)
		subList(index - startExtra, index + endExtra + /* inclusive */ 1)
	} else {
		subList(index - range, index + range + /* inclusive */ 1)
	}
}

/**
 * sublist around the index within the given range by spreading
 * input: listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).subListSpreadFromIndex(4, 2)
 * output: [5, 4, 6, 3, 7]
 *
 * @param index the spread start index
 * @param range the range of the spread
 * @param startFront whether the spread direction should start to the front
 * 	input: listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).subListSpreadFromIndex(4, 2, false)
 * 	output: [5, 6, 4, 7, 3]
 * @param ignoreBounds whether to ignore the spread bounds
 * 	input: listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).subListSpreadFromIndex(4, 10, true, true)
 *  output: [5, 4, 6, 3, 7, 2, 8, 1, 9]
 */
fun <E> List<E>.subListSpreadFromIndex(
	index: Int,
	range: Int,
	startFront: Boolean = true,
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

	/*
	for (i in 0.. iteratorRange * 2) {
		val element = if (i % 2 == 0) {
			get(index + i / 2)
		} else {
			get(index - (i / 2 + 1))
		}
		result.add(element)
	}*/

	val result = mutableListOf<E>()
	var direction = if (startFront) 1 else -1
	var currentIndex = index
	var nextStep = 1
	while (result.size < iteratorRange * 2 + 1) {
		result.add(get(currentIndex))
		direction *= -1
		currentIndex += direction * nextStep++
	}
	return result
}

// operator ?
fun <E> List<E>.multiply(amount: Int): List<E> {
	require(amount >= 0) {
		"Invalid Argument, cannot multiply by negative amount=$amount"
	}
	val result = mutableListOf<E>()
	repeat(amount) { result.addAll(this) }
	return result
}
