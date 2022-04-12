package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.components.util.NoRipple
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.root.BottomNavigationItem
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.root.BottomNavigationRoute
import com.kylentt.musicplayer.ui.activity.musicactivity.compose.theme.md3.AppTypography
import com.kylentt.musicplayer.ui.activity.musicactivity.compose.theme.md3.ColorHelper

@Composable
@Preview
fun RootBottomNavPreview(
    ripple: Boolean = false,
    selected: String =  BottomNavigationRoute.routeList.shuffled()[0].route,
    navigateTo: (String) -> Unit = { },
) {
    RootBottomNav(
        ripple = ripple,
        selectedRoute = selected,
        navigateTo = navigateTo
    )
}

@Composable
fun RootBottomNav(
    ripple: Boolean,
    selectedRoute: String,
    navigateTo: (String) -> Unit,
) {
    val screens = BottomNavigationRoute.routeList
    Surface(
        color = ColorHelper.getTonedSurface()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                screens.forEach { item ->
                    if (ripple) {
                        RootBottomNavItem(
                            item = item,
                            isSelected = selectedRoute == item.route,
                            onClick = navigateTo
                        )
                    } else NoRipple {
                        RootBottomNavItem(
                            item = item,
                            isSelected = selectedRoute == item.route,
                            onClick = navigateTo
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootBottomNavItem(
    item: BottomNavigationItem,
    isSelected: Boolean,
    onClick: (String) -> Unit
) {
    val background = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else Color.Transparent

    val icon = if (isSelected) {
        item.icon ?: item.imageVector()!!
    } else item.outlinedIcon ?: item.imageVector()!!

    val iconTint = if (isSelected) {
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f).compositeOver(Color.White)
    } else {
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f).compositeOver(Color.White)
    }

    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.onSurface
    } else MaterialTheme.colorScheme.onSurfaceVariant

    val route = item.route
    
    Card(
        modifier = Modifier,
        containerColor = background,
        shape = CircleShape
    ) {
        
        Row(
            modifier = Modifier
                .clipToBounds()
                .clickable(true, onClick = { onClick(route) })
                .padding(start = 10.dp, top = 5.dp, bottom = 5.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {

            Icon(
                modifier = Modifier.size(30.dp),
                imageVector = icon,
                contentDescription = item.title,
                tint = iconTint
            )
            
            Spacer(modifier = Modifier.width(2.dp))

            AnimatedVisibility(visible = isSelected) {
                Text(
                    color = textColor,
                    fontWeight = AppTypography.labelMedium.fontWeight,
                    fontSize = AppTypography.bodyMedium.fontSize,
                    fontStyle = AppTypography.labelMedium.fontStyle,
                    lineHeight = AppTypography.labelMedium.lineHeight,
                    text = item.title
                )
            }
        }
    }
}



@Composable
@Preview
fun RBNItem1() {
    RootBottomNavItem(
        item = BottomNavigationItem.Home,
        isSelected = true,
        onClick = {}
    )
}
