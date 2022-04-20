package com.kylentt.mediaplayer.disposed.ui.mainactivity.disposed.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.AppTypography
import com.kylentt.musicplayer.ui.activity.musicactivity.acompose.theme.md3.ColorHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun HomeCard() {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .height(125.dp)
      .clip(RoundedCornerShape(10))
      .shadow(2.dp, shape = RoundedCornerShape(10)),
    color = ColorHelper.getTonedSurface(4)
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
@Preview
fun WhateverCard(
  image: ImageVector = Icons.Default.AccountCircle,
  desc: String = "Example"
) {
  Column(
    modifier = Modifier
      .height(80.dp)
      .width(75.dp)
      .background(Color.Transparent),
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
      elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
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
