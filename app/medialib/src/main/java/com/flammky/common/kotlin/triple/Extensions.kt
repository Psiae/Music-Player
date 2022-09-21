package com.flammky.common.kotlin.triple

infix fun <A, B, C> Pair<A, B>.triple(third: C) = Triple(first, second, third)
