package com.kylentt.mediaplayer.ui.screen.landing

import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.whenResumed
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import com.kylentt.mediaplayer.domain.presenter.util.State.ServiceState
import kotlinx.coroutines.delay


@Composable
fun PermissionScreen(
    navController: NavController,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            StoragePermissionButton()
        }
    }
}

@Composable
@Preview
fun PermissionScreenPreview(navController: NavController? = null) {

}

@Composable
fun StoragePermissionButton() {
    var granted by remember { mutableStateOf(false) }

}


@Composable
fun SplashScreen(navController: NavController, vm: ControllerViewModel) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 5f,
            animationSpec = TweenSpec(
                durationMillis = 2000,
                 easing = {
                     OvershootInterpolator(2f).getInterpolation(it)
                 }
            )
        )
        while (when (vm.serviceState.value) {
                ServiceState.Disconnected, ServiceState.Connecting -> true
                else -> false }
        ) {
            delay(100)
        }
        // This is just a pop up screen like dialog
        // so popBackStack() seems more suitable instead of navigate
        navController.popBackStack()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.play_icon_theme3),
            contentDescription = "Splash"
        )
    }
}