package dev.dexsr.klio.base.ktx.coroutines.pausable

import dev.dexsr.klio.base.ktx.coroutines.SynchronizedObject


internal class PausableDispatchQueue() {

	// kotlin.Any
	private val lock = SynchronizedObject()
	private val queue = ArrayDeque<Runnable>()

	@Volatile
	private var _isPaused = false

	val isPaused: Boolean
		get() = _isPaused


	fun offer(block: Runnable): Boolean {
		if (!isPaused) return false
		return sync {
			if (!isPaused) return false
			enqueue(block)
			true
		}
	}

	fun remove(block: Runnable): Boolean {
		if (!isPaused) return false
		return sync {
			if (!isPaused) return false
			dequeue(block)
		}
	}

	fun pause(): Boolean {
		if (isPaused) return false
		return sync {
			if (isPaused) return false
			_isPaused = true
			true
		}
	}

	fun resume(): Boolean {
		if (!isPaused) return false
		return sync {
			if (!isPaused) return false
			resumeDequeue()
			_isPaused = false
			true
		}
	}

	private fun enqueue(block: Runnable) {
		queue.addLast(block)
	}

	private fun dequeue(block: Runnable): Boolean {
		return queue.remove(block)
	}

	private fun resumeDequeue() {
		queue.forEach { block -> block.run() }
		queue.clear()
	}

	private inline fun <R> sync(block: () -> R) = synchronized(lock, block)
}
