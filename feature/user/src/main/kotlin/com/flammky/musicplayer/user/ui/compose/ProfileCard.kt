package com.flammky.musicplayer.user.ui.compose

import android.graphics.Bitmap
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flammky.androidx.viewmodel.compose.activityViewModel
import com.flammky.musicplayer.base.theme.Theme
import com.flammky.musicplayer.base.theme.compose.ProvideTheme
import com.flammky.musicplayer.base.theme.compose.defaultDarkColorScheme
import com.flammky.musicplayer.base.theme.compose.defaultLightColorScheme
import com.flammky.musicplayer.base.theme.compose.primaryColorAsState
import com.flammky.musicplayer.user.R

@Composable
internal fun UserProfileCard(
	modifier: Modifier = Modifier,
) {
	Row(modifier = modifier.fillMaxWidth()) {
		AvatarCard(viewModel = activityViewModel())
		UserTextDescription(modifier = Modifier.padding(horizontal = 15.dp))
	}
}


@Composable
private fun RowScope.AvatarCard(
	viewModel: ProfileCardViewModel
) {

	val userState = viewModel.observeCurrentUser().collectAsState(null)

	val avatarState = remember {
		mutableStateOf<Bitmap?>(null)
	}

	// initialize via dispatch, so we can keep smooth render
	val user = userState.value

	LaunchedEffect(key1 = user, key2 = viewModel, block = {
		avatarState.value = null
		if (user == null) {
			return@LaunchedEffect
		}
		viewModel.observeUserAvatar(user).collect {
			avatarState.value = it
		}
	})

	val context = LocalContext.current
	val coilReq = remember(avatarState.value) {
		ImageRequest.Builder(context)
			.data(avatarState.value ?: R.drawable.user_avatar)
			.crossfade(true)
			.build()
	}
	Card(
		modifier = Modifier
			.fillMaxHeight()
			.aspectRatio(1f, true)
			.border(1.dp, Theme.primaryColorAsState().value, RoundedCornerShape(50))
			.clip(RoundedCornerShape(50))
	) {
		AsyncImage(
			modifier = Modifier.fillMaxSize(),
			model = coilReq,
			contentDescription = "avatar",
			contentScale = ContentScale.Crop
		)
	}
}

@Composable
private fun UserTextDescription(
	modifier: Modifier
) {
	Column(
		modifier = modifier.fillMaxSize(),
		verticalArrangement = Arrangement.Center
	) {
		Text(text = "USER")
		Spacer(modifier = Modifier.height(3.dp))
		Text(text = "No Description")
	}
}



@Composable
@Preview
private fun Preview() {
	Theme.ProvideTheme(
		isDefaultDark = false,
		lightColorScheme = Theme.defaultLightColorScheme(),
		darkColorScheme = Theme.defaultDarkColorScheme()
	) {
		AsyncImage(
			modifier = Modifier
				.fillMaxHeight()
				.aspectRatio(1f, true),
			model = R.drawable.user_avatar,
			contentDescription = "avatar",
			contentScale = ContentScale.Crop
		)
	}
}

@Composable
@Preview
private fun PreviewDark() {
	Theme.ProvideTheme(
		isDefaultDark = true,
		lightColorScheme = Theme.defaultLightColorScheme(),
		darkColorScheme = Theme.defaultDarkColorScheme()
	) {

	}
}
