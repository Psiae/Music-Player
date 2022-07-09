package com.kylentt.mediaplayer.domain.musiclib

class DependencyBundle {
	private val mDependency: MutableList<Any> = mutableListOf()

	fun provides(vararg objects: Any): DependencyBundle = sync {
		objects.forEach { obj ->
			when (obj) {
				is DependencyBundle -> obj.mDependency.forEach { mDependency.add(it) }
				else -> mDependency.find { it.javaClass == obj.javaClass } ?: mDependency.add(obj)
			}
		}
		this
	}

	@Suppress("UNCHECKED_CAST")
	fun <T> get(cls: Class<T>): T? = sync {
		mDependency.find { it.javaClass == cls }
			?.let { exact ->
				return exact as T
			}
			?: mDependency.forEach { possibleSub ->
				if (cls.isInstance(possibleSub)) { return possibleSub as T }
			}
		null
	}

	fun copy(): DependencyBundle = sync { DependencyBundle().provides(this) }


	private inline fun <T> sync(lock: Any = this, block: () -> T) = synchronized(lock) { block() }
}
