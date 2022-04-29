package com.kylentt.disposed.disposed.ui.mainactivity.disposed.compose.root

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.kylentt.mediaplayer.R
import com.kylentt.disposed.disposed.ui.mainactivity.disposed.compose.screen.Screen
import com.kylentt.disposed.disposed.ui.mainactivity.util.UIConstants

sealed class BottomNavigationItem(
  val route: String,
  val title: String,
  val icon: ImageVector?,
  val outlinedIcon: ImageVector?,
  val imageVector: @Composable () -> ImageVector?
) {

  object Home : BottomNavigationItem(
    route = Screen.HomeScreen.route,
    title = Screen.HomeScreen.name,
    icon = Icons.Default.Home,
    outlinedIcon = Icons.Default.Home,
    imageVector = { null }
  )

  object Search : BottomNavigationItem(
    route = Screen.SearchScreen.route,
    title = Screen.SearchScreen.name,
    icon = Icons.Default.Search,
    outlinedIcon = Icons.Outlined.Search,
    imageVector = { null }
  )

  object Library : BottomNavigationItem(
    route = Screen.LibraryScreen.route,
    title = Screen.LibraryScreen.name,
    icon = null,
    outlinedIcon = null,
    imageVector = { ImageVector.vectorResource(id = R.drawable.ic_bookshelf) }
  )

  // TODO : Other Screen
}

sealed class BottomNavigationRoute(val screen: BottomNavigationItem) {
  object HomeScreen : BottomNavigationRoute(BottomNavigationItem.Home)
  object SearchScreen : BottomNavigationRoute(BottomNavigationItem.Search)
  object LibraryScreen : BottomNavigationRoute(BottomNavigationItem.Library)

  companion object {
    const val routeName = UIConstants.BOTTOM_BAR_ROUTE
    val routeList = listOf(
      BottomNavigationItem.Home, BottomNavigationItem.Search, BottomNavigationItem.Library
    )
  }
}


