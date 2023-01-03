package com.flammky.android.common.bundle

import android.os.Bundle
import android.os.Parcel

fun Bundle.calculateByteSize(): Int {
	val parcel = Parcel.obtain()
	parcel.writeBundle(this)
	return parcel.dataSize().also { parcel.recycle() }
}

fun Bundle.isTooLarge(): Boolean {
	return calculateByteSize() > BundleConstants.limitBytes
}

@kotlin.jvm.Throws(IllegalArgumentException::class)
fun Bundle.requireTransactionSize() {
	require(!isTooLarge()) {
		"Bundle size (${calculateByteSize()}) is out of bounds for transactions, limited to: " +
			"0..${BundleConstants.limitBytes} inclusive"
	}
}

object BundleConstants {
	const val limitBytes = 1_000_000
}
