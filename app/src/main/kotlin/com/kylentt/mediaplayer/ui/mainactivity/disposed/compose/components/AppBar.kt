package com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.components

import android.Manifest
import android.app.WallpaperManager
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.CoilShimmerImage
import com.kylentt.mediaplayer.ui.mainactivity.disposed.compose.CoilShimmerState
import com.kylentt.musicplayer.ui.musicactivity.compose.theme.md3.ColorHelper
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, coil.annotation.ExperimentalCoilApi::class)
@Composable
fun HomeAppBar() {

    Timber.d("ComposeDebug HomeAppBar")
    val context = LocalContext.current
    val perm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    val wp = remember { WallpaperManager.getInstance(context) }
    val data = if (perm) { wp.drawable.toBitmap() } else ContextCompat.getDrawable(context, R.drawable.test_p2)!!.toBitmap()

    val profilePainter =  rememberImagePainter(
        request = ImageRequest.Builder(context)
            .data(data)
            .size(256)
            .build()

    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(75.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Timber.d("ComposeDebug HomeAppBar Row")
        Row(
            modifier = Modifier
                .padding(15.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Timber.d("ComposeDebug HomeAppBar NestedRow")
            Surface(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(50))
                    .clipToBounds(),
                color = Color.Transparent,
                border = BorderStroke((1).dp, MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(50),
                elevation = 1.dp

            ) {
                Timber.d("ComposeDebug HomeAppBar Surface")
                IconButton(
                    onClick = {
                        Toast.makeText(context, "Account", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Timber.d("ComposeDebug HomeAppBar IconButton")
                    if (perm) {
                        CoilShimmerImage(modifier = Modifier.fillMaxSize(),
                            painter = profilePainter,
                            contentDescription = null,
                            contentAlignment = Alignment.CenterStart,
                            shimmerState = CoilShimmerState.LOADING
                        )
                    } else {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds(),
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Account",
                            tint = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(15.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { Toast.makeText(context, "Notification", Toast.LENGTH_SHORT).show() }) {
                Icon(
                    modifier = Modifier.size((27.5).dp),
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notification",
                    tint = ColorHelper.getSurfaceIconTint()
                )
            }
            IconButton(onClick = { Toast.makeText(context, "Settings", Toast.LENGTH_SHORT).show() }) {
                Icon(
                    modifier = Modifier.size((27.5).dp),
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = ColorHelper.getSurfaceIconTint()
                )
            }
        }
    }
}