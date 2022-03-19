package com.kylentt.mediaplayer.ui.mainactivity.compose.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kylentt.mediaplayer.ui.mainactivity.compose.theme.md3.AppTypography
import com.kylentt.mediaplayer.ui.mainactivity.compose.theme.md3.DefaultColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .height(125.dp)
            .clip(RoundedCornerShape(10))
            .shadow(2.dp, shape = RoundedCornerShape(10)),
        color = DefaultColor.getTonedSurface(4)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WhateverCard(
                image = Icons.Default.History,
                desc = "History"
            )
            WhateverCard(
                image = Icons.Default.Favorite,
                desc = "Favorite"
            )
            WhateverCard(
                image = Icons.Default.Shuffle,
                desc = "Shuffle"
            )
            WhateverCard(
                image = Icons.Default.TrendingUp,
                desc = "Most Played"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhateverCard(
    image: ImageVector,
    desc: String
) {
    Column(
        modifier = Modifier
            .height(80.dp)
            .width(75.dp)
        ,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Card(
            modifier = Modifier
                .height(55.dp)
                .width(55.dp)
                .clip(RoundedCornerShape(50))
                .clipToBounds()
                .clickable { },
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Icon(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(15.dp),
                imageVector = image,
                tint = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.surface,
                contentDescription = desc
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = desc,
            fontSize = AppTypography.labelMedium.fontSize,
            fontStyle = AppTypography.labelMedium.fontStyle,
            fontWeight = AppTypography.labelMedium.fontWeight,
            fontFamily = AppTypography.labelMedium.fontFamily,
        )
    }
}