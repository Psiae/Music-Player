package com.kylentt.mediaplayer.ui.activity.mainactivity.compose.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun HomeScreen(
    openSearchScreen: () -> Unit,
    openLibraryScreen: () -> Unit,
    openSettingScreen: () -> Unit,
    openUserScreen: () -> Unit
) {

}

@Composable
private fun Home(
    viewModel: HomeViewModel = hiltViewModel(),
    openSearchScreen: () -> Unit,
    openLibraryScreen: () -> Unit,
    openExtraScreen: () -> Unit,
    openSettingScreen: () -> Unit,
    openUserScreen: () -> Unit
) {

}
