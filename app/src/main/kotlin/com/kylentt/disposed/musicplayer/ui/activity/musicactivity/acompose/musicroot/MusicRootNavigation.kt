package com.kylentt.disposed.musicplayer.ui.activity.musicactivity.acompose.musicroot

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.kylentt.mediaplayer.R
import com.kylentt.disposed.musicplayer.core.helper.UIHelper
import com.kylentt.disposed.musicplayer.ui.UIConstants

sealed class MusicRootNavigation {


    companion object {
        val routeName = UIConstants.ComposeConstants.MUSIC_ROOT_BOTTOM_NAV_ROUTE
        val routeList = listOf(
            MusicRootBottomItem.HomeItem,
            MusicRootBottomItem.SearchItem,
            MusicRootBottomItem.LibraryScreen,
            MusicRootBottomItem.ExtraScreen
        )
        val routeListString = routeList.map { it.routeName }
    }
}

sealed class MusicRootBottomItem(
    val screen: MusicRootScreen,
    val title: String = screen.title,
    val routeName: String = screen.routeName,
    val selectedIcon: ImageVector,
    val notSelectedIcon: ImageVector
) {
    object HomeItem : MusicRootBottomItem(
        screen = MusicRootScreen.HomeScreen,
        selectedIcon = Icons.Default.Home,
        notSelectedIcon = Icons.Outlined.Home
    )

    object SearchItem : MusicRootBottomItem(
        screen = MusicRootScreen.SearchScreen,
        selectedIcon = Icons.Default.Search,
        notSelectedIcon = Icons.Outlined.Search
    )

    object LibraryScreen : MusicRootBottomItem(
        screen = MusicRootScreen.LibraryScreen,
        selectedIcon = UIHelper.getVectorImage(R.drawable.ic_bookshelf),
        notSelectedIcon = UIHelper.getVectorImage(R.drawable.ic_bookshelf)
    )

    object ExtraScreen : MusicRootBottomItem(
        screen = MusicRootScreen.ExtraScreen,
        selectedIcon = Icons.Filled.Star,
        notSelectedIcon = Icons.Outlined.Star
    )

}

sealed class MusicRootScreen(
    val title: String,
    val routeName: String
) {
    object HomeScreen : MusicRootScreen(
        title = UIConstants.ComposeConstants.HOME_SCREEN_TITLE,
        routeName = UIConstants.ComposeConstants.HOME_SCREEN_ROUTE_NAME
    )

    object SearchScreen : MusicRootScreen(
        title = UIConstants.ComposeConstants.SEARCH_SCREEN_TITLE,
        routeName = UIConstants.ComposeConstants.SEARCH_SCREEN_ROUTE_NAME
    )

    object LibraryScreen : MusicRootScreen(
        title = UIConstants.ComposeConstants.LIBRARY_SCREEN_TITLE,
        routeName = UIConstants.ComposeConstants.LIBRARY_SCREEN_ROUTE_NAME
    )

    object ExtraScreen : MusicRootScreen(
        title = "Extra",
        routeName = "EXTRA"
    )

}
