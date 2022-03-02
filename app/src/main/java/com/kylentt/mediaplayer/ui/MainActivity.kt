package com.kylentt.mediaplayer.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.ImageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.kylentt.mediaplayer.R
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_ANDROID
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_COLOROS_FM
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_DRIVE_LEGACY
import com.kylentt.mediaplayer.core.util.Constants.PROVIDER_EXTERNAL_STORAGE
import com.kylentt.mediaplayer.domain.presenter.ControllerViewModel
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.transformers.coil.CropSquareTransformation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        val TAG: String = this::class.java.simpleName
        var isActive = false
    }

    @Inject
    lateinit var coil: ImageLoader

    private val controllerVM: ControllerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isActive = true
        onNewIntent(intent)

        setContent {
            Navigation()
        }

        // Check For Storage Permission
        if (checkPermission()) {
            // TODO: Permission Screen
        }
    }

    private fun checkPermission(): Boolean {
        return true
    }

    // Handle Incoming Intent such as ACTION_VIEW for Audio Files
    // This Function Only called when MainActivity is not Destroyed
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        Timber.d("IntentHandler MainActivity NewIntent")
        intent?.let {
            handleIntent(intent)
        }
    }

    // check the requested action of the Intent
    private fun handleIntent(intent: Intent) {
        Timber.d("IntentHandler MainActivity forwarding Intent ${intent.action}")

        when (intent.action) {
            Intent.ACTION_VIEW -> handleIntentActionView(intent)
        }
    }

    // check for Data Availability then forward it to ControllerVM
    private fun handleIntentActionView(intent: Intent) {
        Timber.d("IntentHandler MainActivity IntentActionView")

        intent.data?.let { uri ->
            Timber.d("IntentHandler MainActivity handling uri scheme ${uri.scheme}")

            val uris = uri.toString()
            when (uri.scheme) {
                "content" -> {
                    when {
                        uris.startsWith(PROVIDER_ANDROID) -> {
                            lifecycleScope.launch { controllerVM.handleDocsIntent(uri) }
                        }
                        uris.startsWith(PROVIDER_DRIVE_LEGACY) -> {
                            lifecycleScope.launch { controllerVM.handleItemIntent(uri) }
                        }
                        uris.startsWith(PROVIDER_EXTERNAL_STORAGE) -> {
                            lifecycleScope.launch { controllerVM.handleDocsIntent(uri) }
                        }
                        uris.startsWith(PROVIDER_COLOROS_FM) -> {
                            lifecycleScope.launch { controllerVM.handleDocsIntent(uri) }
                        }
                        else -> {
                            lifecycleScope.launch { controllerVM.handleDocsIntent(uri) }
                            Toast.makeText(this, "unsupported, please inform us \n " + uris.split("/")[2], Toast.LENGTH_LONG).show()
                        }
                    }
                }
                else -> {
                    Toast.makeText(this, "unsupported, please inform us \n" + uris.split("/")[2], Toast.LENGTH_LONG).show()
                }
            }
        } ?: Timber.e("IntentHandler ActionView null Data")
    }

    private suspend fun Bitmap.squareWithCoil(): Bitmap? {
        val req = ImageRequest.Builder(this@MainActivity)
            .diskCachePolicy(CachePolicy.DISABLED)
            .transformations(CropSquareTransformation())
            .size(256)
            .scale(Scale.FILL)
            .data(this)
            .build()
        return ((coil.execute(req).drawable) as BitmapDrawable?)?.bitmap
    }

    override fun onDestroy() {
        isActive = false
        applicationContext.externalCacheDir?.deleteRecursively()
        applicationContext.cacheDir.deleteRecursively()
        super.onDestroy()
    }
}

@Composable
fun Navigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash_screen") {
        composable("splash_screen") {
            SplashScreen(navController = navController)
        }
        composable("home_screen") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Home Screen", color = Color.LightGray)
            }
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    val scale = remember {
        Animatable(0f)
    }
    LaunchedEffect(key1 = true) {
        scale.animateTo(
            targetValue = 2f,
            animationSpec = tween(
                durationMillis = 500,
                easing = {
                    OvershootInterpolator(2f).getInterpolation(it)
                }
            )
        )
        delay(1000)
        navController.popBackStack()
        navController.navigate("home_screen")
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_3_foreground),
            contentDescription = "Splash",
            modifier = Modifier.scale(scale.value)
        )
    }
}

