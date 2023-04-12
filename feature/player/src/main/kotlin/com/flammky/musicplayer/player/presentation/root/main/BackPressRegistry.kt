package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.runtime.mutableStateListOf

class BackPressRegistry {

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
        fun consume()
    }

    fun hasBackPressConsumer(): Boolean = !backPressConsumers.isEmpty()

    fun consumeBackPress(): Boolean {
        return backPressConsumers.lastOrNull()?.apply { consume() } != null
    }
}