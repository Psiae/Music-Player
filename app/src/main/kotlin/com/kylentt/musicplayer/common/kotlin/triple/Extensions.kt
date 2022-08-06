package com.kylentt.musicplayer.common.kotlin.triple

infix fun <A,B,C> Pair<A,B>.to(third: C) = Triple<A,B,C>(first, second, third)
