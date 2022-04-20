package com.kylentt.mediaplayer.disposed.ui.mainactivity.disposed.compose.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kylentt.mediaplayer.disposed.ui.mainactivity.disposed.compose.components.HomeAppBar
import com.kylentt.mediaplayer.disposed.ui.mainactivity.disposed.compose.components.HomeCard
import com.kylentt.mediaplayer.disposed.ui.mainactivity.disposed.compose.components.util.StatusBarSpacer

@Composable
fun FakeHomeScreen() {
  FakeHomeScreenLayout()
}

@Composable
fun FakeHomeScreenLayout() {
  Column(
    modifier = Modifier
      .fillMaxSize(),
    verticalArrangement = Arrangement.Top,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    StatusBarSpacer()
    HomeAppBar()
    HomeBarCardSpacer()
    HomeCard()
  }
}
