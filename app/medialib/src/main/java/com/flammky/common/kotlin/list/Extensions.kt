package com.flammky.common.kotlin.list

fun <T> List<T>?.orEmpty() = this ?: emptyList()
