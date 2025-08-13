package com.ken.directscreenshare

import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private val TAG="ViewScreen"

@Composable
fun ViewScreen() {
    ComposeSurfaceView(Modifier.fillMaxSize(), object: SurfaceHolder.Callback {
        override fun surfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            Log.i(TAG, "surface changed")
        }

        override fun surfaceCreated(holder: SurfaceHolder) {
            NativeFuncs.startViewingScreen(holder.surface)
            Log.i(TAG, "surface created")
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            Log.e(TAG, "surface destroyed")
        }

    })

}

@Composable
fun ComposeSurfaceView(modifier: Modifier, surfaceCallback: SurfaceHolder.Callback) {
    AndroidView(
        factory = { context: Context ->
            SurfaceView(context).apply {
                holder.addCallback(surfaceCallback)
            }
        },
        modifier = modifier
    )
}
