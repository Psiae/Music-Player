package com.kylentt.mediaplayer.ui.screen.main

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.kylentt.mediaplayer.ui.screen.main.home.HomeScreen
import com.kylentt.mediaplayer.ui.screen.main.search.SearchScreen
import com.kylentt.mediaplayer.ui.theme.md3.TextColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    rootController: NavHostController
) {
    val mainController = rememberNavController()
    Scaffold(
        modifier = Modifier.systemBarsPadding(),
        bottomBar = { MainBottomBar(mainController = mainController) }
    ) {
        MainBottomBarScreen(mainController = mainController)
    }

}

@Composable
fun MainBottomBar(mainController: NavHostController) {
    val screens = listOf(
        BottomBarScreen.Home,
        BottomBarScreen.Search,
    )
    val navBackStackEntry = mainController.currentBackStackEntryAsState()

    CompositionLocalProvider(
        LocalRippleTheme provides object : RippleTheme {

            @Composable
            override fun defaultColor(): Color = Color.Transparent


            @Composable
            override fun rippleAlpha(): RippleAlpha = RippleAlpha(0f,0f,0f,0f)
        }
    ) {
        BottomNavigation(
            elevation = 0.dp,
            backgroundColor = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.05f) else Color.White
        ) {
            val textcolor = if (isSystemInDarkTheme()) Color.White else MaterialTheme.colorScheme.onBackground
            screens.forEach { screen ->
                val selected = navBackStackEntry.value?.destination?.route
                BottomNavigationItem(
                    selected = screen.route == selected,
                    selectedContentColor = textcolor,
                    unselectedContentColor = textcolor.copy(alpha = 0.5f),
                    onClick = { mainController.navigate(screen.route) {
                        popUpTo(BottomBarScreen.Home.route)
                        launchSingleTop = true
                    } },
                    label = {
                        Text(text = screen.title, color = textcolor)
                    },
                    icon = {
                        Icon(imageVector = screen.icon, contentDescription = screen.title)
                    }
                )
            }
        }
    }
}


@Composable
fun MainBottomBarScreen(mainController: NavHostController) {
    NavHost(
        navController = mainController,
        startDestination = BottomBarScreen.Home.route
    ) {
        composable(route = BottomBarScreen.Home.route) {
            HomeScreen(navController = mainController)
        }
        composable(route = BottomBarScreen.Search.route) {
            SearchScreen(navController = mainController)
        }
    }
}