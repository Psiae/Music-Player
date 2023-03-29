package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.player.R

@Composable
fun PlaybackControlToolBar(
    state: PlaybackControlToolBarState
) = state.coordinator.ComposeContent(
    dismissContents = {
        dismissIconRenderFactory { modifier ->
            Icon(
                modifier = modifier,
                painter = painterResource(id = R.drawable.ios_glyph_expand_arrow_down_100),
                contentDescription = "close",
                tint = tint()
            )
        }
    },
    menuContents = {
        moreMenuIconRenderFactory { modifier ->
            Icon(
                modifier = modifier,
                painter = painterResource(id = com.flammky.musicplayer.base.R.drawable.more_vert_48px),
                contentDescription = "more",
                tint = tint()
            )
        }
    }
)

class PlaybackControlToolBarState(
   onDismissClick: () -> Unit
) {

    val coordinator = PlaybackControlToolBarCoordinator(onDismissClick)
}

class PlaybackControlToolBarCoordinator(
    private val onDismissClick: () -> Unit
) {

    interface DismissContentScope {
        fun dismissIconRenderFactory(
            content: @Composable DismissIconRenderScope.(modifier: Modifier) -> Unit
        )
    }

    interface DismissIconRenderScope {
        @Composable
        fun tint(): Color
    }

    interface MenuContentScope {
        fun moreMenuIconRenderFactory(
            content: @Composable MoreMenuIconRenderScope.(modifier: Modifier) -> Unit
        )
    }

    interface MoreMenuIconRenderScope {
        @Composable
        fun tint(): Color
    }

    private class DismissContentRenderScopeImpl() {

        val icon = object : DismissIconRenderScope {

            @Composable
            override fun tint(): Color = Theme.backgroundContentColorAsState().value
        }
    }

    private class MenuContentRenderScopeImpl() {

        val icon = object : MoreMenuIconRenderScope {

            @Composable
            override fun tint(): Color = Theme.backgroundContentColorAsState().value
        }
    }

    private class DismissContentScopeImpl(): DismissContentScope {

        private val renderScope = DismissContentRenderScopeImpl()

        var dismissIcon by mutableStateOf<@Composable () -> Unit>({})
            private set

        private var getDismissIconLayoutModifier by mutableStateOf<Modifier.() -> Modifier>({ Modifier })

        fun attachLayoutHandle(
            getDismissIconLayoutModifier: Modifier.() -> Modifier
        ) {
            this.getDismissIconLayoutModifier = getDismissIconLayoutModifier
        }

        override fun dismissIconRenderFactory(
            content: @Composable DismissIconRenderScope.(modifier: Modifier) -> Unit
        ) {
            dismissIcon = @Composable { renderScope.icon.content(Modifier.getDismissIconLayoutModifier()) }
        }
    }

    private class MenuContentScopeImpl(): MenuContentScope {

        private val renderScope = MenuContentRenderScopeImpl()

        var moreMenuIcon by mutableStateOf<@Composable () -> Unit>({})
            private set

        private var getMoreMenuIconLayoutModifier by mutableStateOf<Modifier.() -> Modifier>({ Modifier })

        fun attachLayoutHandle(
            getMoreMenuIconLayoutModifier: Modifier.() -> Modifier
        ) {
            this.getMoreMenuIconLayoutModifier = getMoreMenuIconLayoutModifier
        }

        override fun moreMenuIconRenderFactory(
            content: @Composable MoreMenuIconRenderScope.(modifier: Modifier) -> Unit
        ) {
            moreMenuIcon = @Composable { renderScope.icon.content(Modifier.getMoreMenuIconLayoutModifier()) }
        }
    }

    val layoutCoordinator = PlaybackControlToolBarLayoutCoordinator()

    @Composable
    fun ComposeContent(
        dismissContents: DismissContentScope.() -> Unit,
        menuContents: MenuContentScope.() -> Unit,
    ) {
        with(layoutCoordinator) {
            val dismissContentScope = rememberDismissContentScope().apply(dismissContents)
            val menuContentScope = rememberMenuContentScope().apply(menuContents)
            PlaceToolbar(
                dismissContents = {
                    dismissContentScope.attachLayoutHandle(
                        getDismissIconLayoutModifier = { dismissIconLayoutModifier() }
                    )
                    setDismissClickListener(onDismissClick)
                    setDismissIconFactory { dismissContentScope.dismissIcon() }
                },
                menuContents = {
                    menuContentScope.attachLayoutHandle(
                        getMoreMenuIconLayoutModifier = { moreMenuIconLayoutModifier() }
                    )
                    setMoreMenuIconFactory { menuContentScope.moreMenuIcon() }
                }
            )
        }
    }


    @Composable
    private fun rememberDismissContentScope(): DismissContentScopeImpl {
        return remember(this) {
            DismissContentScopeImpl()
        }
    }

    @Composable
    private fun rememberMenuContentScope(): MenuContentScopeImpl {
        return remember(this) {
            MenuContentScopeImpl()
        }
    }
}

class PlaybackControlToolBarLayoutCoordinator {

    interface DismissButtonLayoutScope {
        fun Modifier.dismissIconLayoutModifier(): Modifier
        fun setDismissClickListener(onDismissClick: () -> Unit)
        fun setDismissIconFactory(content: @Composable () -> Unit)
    }

    interface MenuButtonLayoutScope {
        fun Modifier.moreMenuIconLayoutModifier(): Modifier
        fun setMoreMenuIconFactory(content: @Composable () -> Unit)
    }

    private class DismissContentScopeImpl(
        val interactionSource: InteractionSource,
    ): DismissButtonLayoutScope {

        private var _dismissIconFactory by mutableStateOf<@Composable () -> Unit>({})

        private var _dismissClickListener by mutableStateOf<() -> Unit>({})

        val dismissIconFactory
            get() = _dismissIconFactory

        val dismissClickListener
            get() = _dismissClickListener


        override fun Modifier.dismissIconLayoutModifier(): Modifier {
            return composed {
                if (interactionSource.collectIsPressedAsState().value) {
                    size(24.dp)
                } else {
                    size(26.dp)
                }
            }
        }

        override fun setDismissClickListener(onDismissClick: () -> Unit) {
            this._dismissClickListener = onDismissClick
        }

        override fun setDismissIconFactory(content: @Composable () -> Unit) {
            this._dismissIconFactory = @Composable { content() }
        }
    }

    private class MenuContentScopeImpl(
       val moreMenuInteractionSource: InteractionSource
    ): MenuButtonLayoutScope {

        var menuIconFactory by mutableStateOf<@Composable () -> Unit>({})
            private set

        override fun Modifier.moreMenuIconLayoutModifier(): Modifier {
            return composed {
                if (moreMenuInteractionSource.collectIsPressedAsState().value) {
                    size(24.dp)
                } else {
                    size(26.dp)
                }
            }
        }

        override fun setMoreMenuIconFactory(content: @Composable () -> Unit) {
            this.menuIconFactory = @Composable { content() }
        }
    }

    @Composable
    fun PlaceToolbar(
        dismissContents: DismissButtonLayoutScope.() -> Unit,
        menuContents: MenuButtonLayoutScope.() -> Unit
    ) = BoxWithConstraints(Modifier.fillMaxWidth()) {
        val dismissContentScope = remember {
            DismissContentScopeImpl(
                interactionSource = MutableInteractionSource()
            )
        }.apply(dismissContents)
        val menuContentsScope = remember {
            MenuContentScopeImpl(
                moreMenuInteractionSource = MutableInteractionSource()
            )
        }.apply(menuContents)
        Row(
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(35.dp)
                    .clickable(
                        interactionSource = dismissContentScope.interactionSource as MutableInteractionSource,
                        indication = null,
                        enabled = true,
                        onClickLabel = null,
                        role = null,
                        onClick = dismissContentScope.dismissClickListener
                    ),
                contentAlignment = Alignment.Center
            ) {
                dismissContentScope.dismissIconFactory()
            }
            Spacer(modifier = Modifier.weight(2f))
            menuContentsScope.menuIconFactory()
        }
    }
}