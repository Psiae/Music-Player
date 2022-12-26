package com.flammky.musicplayer.dump.mediaplayer.ui.activity.mainactivity.compose.home

import androidx.compose.runtime.Composable
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
