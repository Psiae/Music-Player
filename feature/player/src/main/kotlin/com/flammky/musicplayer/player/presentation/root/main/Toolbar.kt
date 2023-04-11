package com.flammky.musicplayer.player.presentation.root.main

import androidx.compose.foundation.clickable
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
import com.flammky.musicplayer.base.compose.SnapshotReader
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.backgroundContentColorAsState
import com.flammky.musicplayer.player.R
import com.flammky.musicplayer.player.presentation.root.runRemember
import dev.flammky.compose_components.core.SnapshotRead

@Composable
fun rememberPlaybackControlToolBarState(
    dismiss: () -> Unit,
): PlaybackControlToolBarState {
    val upDismiss = rememberUpdatedState(dismiss)
    return remember {
        PlaybackControlToolBarState(
            onDismissClick = { upDismiss.value }
        )
    }
}

@Composable
fun PlaybackControlToolBar(
    state: PlaybackControlToolBarState
) = state.coordinator.ComposeContent(
    dismissContents = @SnapshotRead {
        provideDismissButtonRenderFactory { modifier ->
            Box(modifier = modifier.containerModifier()) {
                Icon(
                    modifier = Modifier
                        .iconModifier()
                        .align(Alignment.Center),
                    painter = painterResource(id = R.drawable.ios_glyph_expand_arrow_down_100),
                    contentDescription = "close",
                    tint = iconTint()
                )
            }
        }
    },
    menuContents = @SnapshotRead {
        provideMoreMenuButtonRenderFactory { modifier ->
            Box(modifier = modifier) {
                Icon(
                    modifier = Modifier
                        .iconModifier()
                        .align(Alignment.Center),
                    painter = painterResource(id = com.flammky.musicplayer.base.R.drawable.more_vert_48px),
                    contentDescription = "more",
                    tint = tint()
                )
            }
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
        fun provideDismissButtonRenderFactory(
            content: @Composable DismissButtonRenderScope.(modifier: Modifier) -> Unit
        )
    }

    interface DismissButtonRenderScope {

        fun Modifier.containerModifier(): Modifier

        fun Modifier.iconModifier(): Modifier

        @Composable
        fun iconTint(): Color
    }

    interface MenuContentScope {
        fun provideMoreMenuButtonRenderFactory(
            content: @Composable MoreMenuIconRenderScope.(modifier: Modifier) -> Unit
        )
    }

    interface MoreMenuIconRenderScope {
        fun Modifier.iconModifier(): Modifier
        @Composable
        fun tint(): Color
    }

    private class DismissContentRenderScopeImpl(
        private val onDismissClicked: () -> Unit,
    ) {

        val button = object : DismissButtonRenderScope {

            val interactionSource = MutableInteractionSource()

            override fun Modifier.containerModifier(): Modifier {
                return size(40.dp)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = true,
                        onClick = onDismissClicked
                    )
            }

            override fun Modifier.iconModifier(): Modifier {
                return composed {
                    size(if (interactionSource.collectIsPressedAsState().value) 26.dp else 28.dp)
                }
            }

            @Composable
            override fun iconTint(): Color = Theme.backgroundContentColorAsState().value
        }
    }

    private class MenuContentRenderScopeImpl() {

        val icon = object : MoreMenuIconRenderScope {

            override fun Modifier.iconModifier(): Modifier {
                return composed { size(28.dp) }
            }

            @Composable
            override fun tint(): Color = Theme.backgroundContentColorAsState().value
        }
    }

    private class DismissContentScopeImpl(
        onDismissClick: (() -> Unit)?
    ): DismissContentScope {

        private val renderScope = DismissContentRenderScopeImpl(onDismissClick ?: {})

        var button by mutableStateOf<@Composable () -> Unit>({})
            private set

        private var getDismissButtonLayoutModifier by mutableStateOf<Modifier.() -> Modifier>({ Modifier })

        fun attachLayoutHandle(
            getDismissIconLayoutModifier: Modifier.() -> Modifier
        ) {
            this.getDismissButtonLayoutModifier = getDismissIconLayoutModifier
        }

        override fun provideDismissButtonRenderFactory(
            content: @Composable DismissButtonRenderScope.(modifier: Modifier) -> Unit
        ) {
            button = @Composable {
                renderScope.button.content(Modifier.getDismissButtonLayoutModifier())
            }
        }
    }

    private class MenuContentScopeImpl(): MenuContentScope {

        private val renderScope = MenuContentRenderScopeImpl()

        var moreMenu by mutableStateOf<@Composable () -> Unit>({})
            private set

        private var getMoreMenuIconLayoutModifier by mutableStateOf<Modifier.() -> Modifier>({ Modifier })

        fun attachLayoutHandle(
            getMoreMenuIconLayoutModifier: Modifier.() -> Modifier
        ) {
            this.getMoreMenuIconLayoutModifier = getMoreMenuIconLayoutModifier
        }

        override fun provideMoreMenuButtonRenderFactory(
            content: @Composable MoreMenuIconRenderScope.(modifier: Modifier) -> Unit
        ) {
            moreMenu = @Composable { renderScope.icon.content(Modifier.getMoreMenuIconLayoutModifier()) }
        }
    }

    val layoutCoordinator = PlaybackControlToolBarLayoutCoordinator()

    @Composable
    fun ComposeContent(
        dismissContents: @SnapshotReader DismissContentScope.() -> Unit,
        menuContents: @SnapshotReader MenuContentScope.() -> Unit,
    ) {
        val upDismissContents = rememberUpdatedState(dismissContents)
        val upMenuContents = rememberUpdatedState(menuContents)
        val dismissContentScope = remember(this) {
            val impl = DismissContentScopeImpl(onDismissClick)
            derivedStateOf { impl.apply(upDismissContents.value) }
        }.value
        val menuContentScope = remember(this) {
            val impl = MenuContentScopeImpl()
            derivedStateOf { impl.apply(upMenuContents.value) }
        }.value
        with(layoutCoordinator) {
            PlaceToolbar(
                dismissButton = @SnapshotRead {
                    dismissContentScope.attachLayoutHandle(
                        getDismissIconLayoutModifier = { dismissIconLayoutModifier() }
                    )
                    provideLayoutData(dismissContentScope.button)
                },
                moreMenuButton = @SnapshotRead {
                    menuContentScope.attachLayoutHandle(
                        getMoreMenuIconLayoutModifier = { moreMenuIconLayoutModifier() }
                    )
                    provideLayoutData(menuContentScope.moreMenu)
                }
            )
        }
    }
}

class PlaybackControlToolBarLayoutCoordinator {

    interface DismissButtonLayoutScope {
        fun Modifier.dismissIconLayoutModifier(): Modifier
        fun provideLayoutData(
            button: @Composable () -> Unit
        )
    }

    interface MenuButtonLayoutScope {
        fun Modifier.moreMenuIconLayoutModifier(): Modifier
        fun provideLayoutData(
            moreMenuButton: @Composable () -> Unit
        )
    }

    private class DismissLayoutData(
        val button: @Composable () -> Unit
    )

    private class MenuLayoutData(
        val menu: @Composable () -> Unit
    )

    private class DismissContentScopeImpl(): DismissButtonLayoutScope {

        var layoutData by mutableStateOf<DismissLayoutData?>(null)

        override fun Modifier.dismissIconLayoutModifier(): Modifier {
            return composed { this }
        }

        override fun provideLayoutData(
            button: @Composable () -> Unit
        ) {
            this.layoutData = DismissLayoutData(button = button)
        }
    }

    private class MenuContentScopeImpl(): MenuButtonLayoutScope {

        var layoutData by mutableStateOf<MenuLayoutData?>(null)
            private set

        override fun Modifier.moreMenuIconLayoutModifier(): Modifier {
            return composed { this }
        }

        override fun provideLayoutData(
            moreMenuButton: @Composable () -> Unit
        ) {
            this.layoutData = MenuLayoutData(menu = moreMenuButton)
        }
    }

    @Composable
    fun PlaceToolbar(
        dismissButton: @SnapshotReader DismissButtonLayoutScope.() -> Unit,
        moreMenuButton: @SnapshotReader MenuButtonLayoutScope.() -> Unit
    ) = BoxWithConstraints(Modifier.fillMaxWidth()) {
        val dismissContentScope = remember(this) {
            DismissContentScopeImpl()
        }.runRemember(dismissButton) {
            derivedStateOf { apply(dismissButton) }
        }.value
        val menuContentsScope = remember(this) {
            MenuContentScopeImpl()
        }.runRemember(moreMenuButton) {
            derivedStateOf { apply(moreMenuButton) }
        }.value
        Row(
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dismissContentScope.layoutData?.button?.let { it() }
            Spacer(modifier = Modifier.weight(2f))
            menuContentsScope.layoutData?.menu?.let { it() }
        }
    }
}