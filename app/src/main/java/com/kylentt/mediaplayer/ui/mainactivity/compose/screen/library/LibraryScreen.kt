package com.kylentt.mediaplayer.ui.mainactivity.compose.screen.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kylentt.mediaplayer.ui.mainactivity.compose.components.util.StatusBarSpacer
import com.kylentt.mediaplayer.ui.mainactivity.compose.theme.md3.DefaultColor

@Composable
fun LibraryScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        StatusBarSpacer()
        Text(text = "Library Screen", color = DefaultColor.getDNTextColor())
        Spacer(modifier = Modifier.height(1000.dp))
        Text(text = "End Of Column", color = DefaultColor.getDNTextColor())
    }
}