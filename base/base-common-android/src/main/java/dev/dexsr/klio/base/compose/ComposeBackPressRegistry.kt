package dev.dexsr.klio.base.compose

import androidx.annotation.MainThread
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import dev.dexsr.klio.android.base.checkInMainLooper

// do we really need this ?
class ComposeBackPressRegistry {

    private val backPressConsumerCount = mutableIntStateOf(0)
    private val backPressConsumers = mutableListOf<BackPressConsumer>()

    @MainThread
    fun registerBackPressConsumer(
        consumer: BackPressConsumer
    ) {
        checkInMainLooper()
        if (backPressConsumers.add(consumer)) {
            Snapshot.withoutReadObservation {
                check(++backPressConsumerCount.intValue == backPressConsumers.size) {
                    "backPressConsumerCount imbalance"
                }
            }
        }
    }

    @MainThread
    fun unregisterBackPressConsumer(
        consumer: BackPressConsumer
    ) {
        checkInMainLooper()
        if (backPressConsumers.remove(consumer)) {
            Snapshot.withoutReadObservation {
                check(--backPressConsumerCount.intValue == backPressConsumers.size) {
                    "backPressConsumerCount imbalance"
                }
            }
        }
    }

    fun interface BackPressConsumer {
        fun consume(): Boolean
    }

    @SnapshotRead
    fun hasBackPressConsumer(): Boolean {
        return backPressConsumerCount.intValue > 0
    }

    fun consumeBackPress(): Boolean {
		for (i in backPressConsumers.indices.reversed()) {
			if (backPressConsumers[i].consume()) return true
		}
		return false
    }
}

val LocalComposeBackPressRegistry = compositionLocalOf<ComposeBackPressRegistry?> { null }
