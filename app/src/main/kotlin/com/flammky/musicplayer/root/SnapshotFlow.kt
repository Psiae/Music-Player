package com.flammky.musicplayer.root

import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> snapshotFlow(
	equality: (old: T, new: T) -> Boolean,
	block: () -> T,
): Flow<T> = flow {
	// Objects read the last time block was run
	val readSet = mutableSetOf<Any>()
	val readObserver: (Any) -> Unit = { readSet.add(it) }

	// This channel may not block or lose data on a trySend call.
	val appliedChanges = Channel<Set<Any>>(Channel.UNLIMITED)

	// Register the apply observer before running for the first time
	// so that we don't miss updates.
	val unregisterApplyObserver = Snapshot.registerApplyObserver { changed, _ ->
		appliedChanges.trySend(changed)
	}

	try {
		var lastValue = Snapshot.takeSnapshot(readObserver).run {
			try {
				enter(block)
			} finally {
				dispose()
			}
		}
		emit(lastValue)

		while (true) {
			var found = false
			var changedObjects = appliedChanges.receive()

			// Poll for any other changes before running block to minimize the number of
			// additional times it runs for the same data
			while (true) {
				// Assumption: readSet will typically be smaller than changed
				found = found || readSet.intersects(changedObjects)
				changedObjects = appliedChanges.tryReceive().getOrNull() ?: break
			}

			if (found) {
				readSet.clear()
				val newValue = Snapshot.takeSnapshot(readObserver).run {
					try {
						enter(block)
					} finally {
						dispose()
					}
				}

				if (!equality(lastValue, newValue)) {
					lastValue = newValue
					emit(newValue)
				}
			}
		}
	} finally {
		unregisterApplyObserver.dispose()
	}
}

/**
 * Return `true` if there are any elements shared between `this` and [other]
 */
private fun <T> Set<T>.intersects(other: Set<T>): Boolean =
	if (size < other.size) any { it in other } else other.any { it in this }
