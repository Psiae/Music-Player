package com.flammky.musicplayer.base.nav.compose

import com.flammky.kotlin.common.lazy.LazyConstructor
import kotlinx.collections.immutable.ImmutableList

object ComposeRootNavigation {
	private val _navigators = LazyConstructor<List<ComposeRootNavigator>>()

	val navigators: List<ComposeRootNavigator> by _navigators.lazy

	fun getNavigatorById(id: String): ComposeRootNavigator? = navigators.find { it.id == id }

	fun provideNavigator(value: ImmutableList<ComposeRootNavigator>) = _navigators.constructOrThrow(
		lazyValue = {
			value
		},
		lazyThrow = {
			error("ComposeRootNavigation was already provided")
		}
	)
}
