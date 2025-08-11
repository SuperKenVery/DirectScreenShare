package com.ken.directscreenshare

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@Composable
fun SelectModeScreen(isSharingScreen: MutableState<Boolean>, navController: NavController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
        ScreenLongSide(shortAlignment = Alignment.CenterHorizontally, longArrangement = Arrangement.SpaceAround, modifier = Modifier.fillMaxSize()) {

            Action(
                {
                    isSharingScreen.value = true
                    navController.navigate(Route.SelectPeer.route)
                },
                R.drawable.share_screen, "Share your screen")

            Action(
                {
                    isSharingScreen.value = false
                    navController.navigate(Route.SelectPeer.route)
                },
                R.drawable.view_screen, "View other's screen"
            )

        }
    }
}

@Composable
fun Action(action: () -> Unit, @DrawableRes icon: Int, description: String) {
    OutlinedButton (
        action,
        shape= RoundedCornerShape(20.dp),
        modifier = Modifier.widthIn(100.dp, 250.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(icon),
                contentDescription = description,
                modifier = Modifier
                    .aspectRatio(1.0f)
            )
            Text(description)
        }
    }
}

@Composable
fun ScreenLongSide(
    longArrangement: Arrangement.HorizontalOrVertical = Arrangement.Center,
    shortAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    modifier: Modifier,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(
            verticalAlignment = shortAlignment.toVertical(),
            horizontalArrangement = longArrangement,
            modifier = modifier
        ) { content() }
    } else {
        Column(
            horizontalAlignment = shortAlignment,
            verticalArrangement = longArrangement,
            modifier = modifier
        ) { content() }
    }
}

fun Alignment.Vertical.toHorizontal(): Alignment.Horizontal = when (this) {
    Alignment.Top -> Alignment.Start
    Alignment.CenterVertically -> Alignment.CenterHorizontally
    Alignment.Bottom -> Alignment.End
    else -> throw IllegalArgumentException("Unknown vertical alignment: $this")
}
fun Alignment.Horizontal.toVertical(): Alignment.Vertical = when (this) {
    Alignment.Start -> Alignment.Top
    Alignment.CenterHorizontally -> Alignment.CenterVertically
    Alignment.End -> Alignment.Bottom
    else -> throw IllegalArgumentException("Unknown horizontal alignment: $this")
}


@Preview
@Preview(device = Devices.AUTOMOTIVE_1024p, widthDp = 720, heightDp = 360)
@Composable
private fun PreviewSelectMode() {
    val isSharing = remember { mutableStateOf(true) }
    val navController = rememberNavController()

    SelectModeScreen(isSharing, navController)
}


