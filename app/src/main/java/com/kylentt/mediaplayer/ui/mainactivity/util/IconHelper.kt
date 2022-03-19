package com.kylentt.mediaplayer.ui.mainactivity.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.kylentt.mediaplayer.R

object IconHelper {
    @Composable
    fun getShelfIcon() = ImageVector.vectorResource(id = R.drawable.ic_bookshelf)
}