package dev.dexsr.klio.base.di.koin.compose

import androidx.compose.runtime.Composable
import org.koin.androidx.compose.get

@Composable
inline fun <reified T> getFromKoin(): T = get()
