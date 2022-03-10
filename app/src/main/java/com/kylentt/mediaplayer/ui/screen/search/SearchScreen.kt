package com.kylentt.mediaplayer.ui.screen.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.kylentt.mediaplayer.ui.theme.md3.DefaultColor
import timber.log.Timber

@Composable
fun SearchScreen(
    vm: SearchViewModel = hiltViewModel()
) {
    Timber.d("ComposeDebug SearchScreen")
    val songList by vm.songList.collectAsState()
    val textColor = DefaultColor.getDNTextColor()

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Search Screen", color = textColor)
        Text(text = "Local Song Found: ${songList.size}", color = textColor)
    }
}