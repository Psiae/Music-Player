package com.kylentt.mediaplayer.ui.screen.Library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kylentt.mediaplayer.ui.theme.md3.DefaultColor

@Composable
fun LibraryScreen() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
           text = "Library Screen", color = DefaultColor.getDNTextColor()
        )
    }
}