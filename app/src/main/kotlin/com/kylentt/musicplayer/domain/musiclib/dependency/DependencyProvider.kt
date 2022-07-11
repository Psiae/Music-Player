package com.kylentt.musicplayer.domain.musiclib.dependency

open class DependencyProvider private constructor() {
	protected open val dependencies = mutableListOf<Any>()

	constructor(vararg objects: Any) : this() {
		provide(*objects)
	}

	protected fun provide(vararg objects: Any) {
		objects.forEach { obj ->
			if (obj is DependencyProvider) {
				obj.getAll().forEach { provide(it) }
			}
			dependencies.find { it.javaClass == obj.javaClass } ?: dependencies.add(obj)
		}
	}

	@Suppress("UNCHECKED_CAST")
	fun <R : Any> get(cls: Class<R>): R? {
		dependencies.forEach { dep -> if (dep.javaClass == cls) return dep as R }
		dependencies.forEach { dep -> if (cls.isInstance(dep)) return dep as /* Super */ R }
		return null
	}

	protected open fun getAll(): List<Any> = dependencies.toList()

	fun getDebugMessage(full: Boolean): String {
		return "$this Debug:" +
			"\ndependency size: ${dependencies.size}" +
			if (full) dependencies.toString() else ""
	}

	open class Mutable(vararg init: Any) : DependencyProvider(*init) {

		open val toImmutable
			get() = DependencyProvider(this)

		fun add(vararg objects: Any) = provide(*objects)
	}
}
