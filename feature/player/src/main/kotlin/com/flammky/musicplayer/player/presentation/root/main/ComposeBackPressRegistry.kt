package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.util.fastLastOrNull

class ComposeBackPressRegistry {

    private var backPressConsumers = mutableStateListOf<BackPressConsumer>()

    fun registerBackPressConsumer(
        consumer: BackPressConsumer
    ) {
        backPressConsumers.add(consumer)
    }

    fun unregisterBackPressConsumer(
        consumer: BackPressConsumer
    ) {
        backPressConsumers.remove(consumer)
    }

    fun interface BackPressConsumer {
        fun consume(): Boolean
    }

    fun hasBackPressConsumer(): Boolean = !backPressConsumers.isEmpty()

    fun consumeBackPress(): Boolean {
        return backPressConsumers.fastLastOrNull { it.consume() } != null
    }
}