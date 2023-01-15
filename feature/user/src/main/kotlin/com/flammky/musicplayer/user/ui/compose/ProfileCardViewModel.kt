package com.flammky.musicplayer.user.ui.compose

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.flammky.musicplayer.base.auth.AuthService
import com.flammky.musicplayer.base.user.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
internal class ProfileCardViewModel @Inject constructor(
	private val auth: AuthService
) : ViewModel() {

	val currentUser: User?
		get() = auth.currentUser

	fun cachedUserAvatar(user: User): Bitmap? {
		return null
	}

	fun observeUserAvatar(user: User): Flow<Bitmap?> {
		return flow {  }
	}

	fun observeUsername(user: User): Flow<String> {
		return flow {  }
	}

	fun observeUserDescription(user: User): Flow<String> {
		return flow {  }
	}

	fun observeCurrentUser(): Flow<User?> = auth.observeCurrentUser()
}
