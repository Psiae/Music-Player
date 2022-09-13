package com.kylentt.musicplayer.common.kotlin.collection

fun <E> Collection<E>.containsReferential(element: E) = find { it === element } !== null
