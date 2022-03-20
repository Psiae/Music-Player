package com.kylentt.mediaplayer.ui.mainactivity.compose.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.Coil
import coil.compose.ImagePainter
import coil.compose.rememberImagePainter
import coil.request.ImageRequest
import coil.size.SizeResolver
import com.google.accompanist.drawablepainter.DrawablePainter
import com.google.accompanist.placeholder.PlaceholderDefaults
import com.google.accompanist.placeholder.PlaceholderHighlight
import com.google.accompanist.placeholder.placeholder
import com.google.accompanist.placeholder.shimmer
import com.kylentt.mediaplayer.ui.mainactivity.compose.theme.md3.DefaultColor
import com.kylentt.mediaplayer.R
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class, coil.annotation.ExperimentalCoilApi::class)
@Composable
fun HomeAppBar() {

    Timber.d("ComposeDebug HomeAppBar")

    val context = LocalContext.current
    val profilePainter = rememberImagePainter(
        request = ImageRequest.Builder(context)
            .data(ContextCompat.getDrawable(context, R.drawable.test_p2)!!)
            .size(128)
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
                    if (true) {
                        Image(modifier = Modifier
                            .fillMaxSize()
                            .placeholder(
                                visible = profilePainter.state is ImagePainter.State.Loading,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                                highlight = PlaceholderHighlight.Companion.shimmer(
                                    highlightColor = MaterialTheme.colorScheme.surface
                                )
                            ),
                            painter = profilePainter,
                            contentDescription = "TestAccount",
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .clipToBounds(),
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = "Account",
                            tint = DefaultColor.getSurfaceIconTint()
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
                    tint = DefaultColor.getSurfaceIconTint()
                )
            }
            IconButton(onClick = { Toast.makeText(context, "Settings", Toast.LENGTH_SHORT).show() }) {
                Icon(
                    modifier = Modifier.size((27.5).dp),
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = DefaultColor.getSurfaceIconTint()
                )
            }
        }
    }
}