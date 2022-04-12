package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.dp
import com.kylentt.mediaplayer.R
import com.kylentt.musicplayer.ui.activity.musicactivity.compose.theme.md3.AppTypography
import com.kylentt.musicplayer.ui.activity.musicactivity.compose.theme.md3.ColorDefaults
import com.kylentt.musicplayer.ui.activity.musicactivity.compose.theme.md3.ColorHelper

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ProfileCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(10),
        elevation = CardDefaults.elevatedCardElevation(),
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        ProfileCardRow()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCardRow() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, top = 15.dp, bottom = 15.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileCardProfile()
        Spacer(modifier = Modifier.width(10.dp))
        ProfileCardColumn()
    }
}

@Composable
fun ProfileCardColumn() {
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        ProfileWelcomeText()
    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun ProfileWelcomeText() {
    Text(
        text = "Hello",
        color = ColorHelper.getDNTextColor(),
        style = AppTypography.titleMedium
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCardProfile() {
    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .fillMaxHeight(),
        shape = RoundedCornerShape(50),
        containerColor = ColorDefaults.white,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.elevatedCardElevation(),
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(
                id = R.drawable.test_p2
            ),
            contentDescription = "Profile",
            contentScale = ContentScale.Crop
        )
    }
}