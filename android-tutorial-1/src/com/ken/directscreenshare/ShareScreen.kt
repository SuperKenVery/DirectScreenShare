package com.ken.directscreenshare

import android.R
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.NotificationCompat
import androidx.window.layout.WindowMetricsCalculator
import android.app.Service;
import android.content.pm.ServiceInfo
import android.graphics.Rect
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResult
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import org.freedesktop.gstreamer.GStreamer


private const val TAG = "ShareScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun ShareScreen() {
    val activity = LocalActivity.current ?: return
    val mediaProjectionManager = remember { activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }

    val startMediaProjection = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if(result.resultCode==Activity.RESULT_OK && result.data != null){
//            val mediaProjection = mediaProjectionManager.getMediaProjection(result.resultCode, result.data!!) ?: return@rememberLauncherForActivityResult
            val metrics = WindowMetricsCalculator.getOrCreate().computeMaximumWindowMetrics(activity)
            val serviceIntent = Intent(activity, ScreenCaptureService::class.java).apply {
                putExtra("result", result)
                putExtra("bounds", metrics.bounds)
                putExtra("dpi", metrics.density.toInt())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                activity.startForegroundService(serviceIntent)
            } else {
                activity.startService(serviceIntent)
            }
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                colors = topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                ),
                title = {
                    Text("Share your screen")
                }
            )
        }
    ) { innerPadding ->
        Button(onClick = {
            GStreamer.init(activity)

            startMediaProjection.launch(mediaProjectionManager.createScreenCaptureIntent())
        }, modifier = Modifier.padding(innerPadding)) {
            Text("Record screen")
        }
    }
}

fun setupScreenCapture(bounds: Rect, dpi: Int, mediaProjection: MediaProjection, windowManager: WindowManager): VirtualDisplay? {
    Log.i(TAG, "ScreenCapture size: $bounds")

    mediaProjection.registerCallback(object: MediaProjection.Callback() {
        override fun onCapturedContentResize(width: Int, height: Int) {
            Log.w(TAG, "Screen capture size changed: $width x $height")
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            Log.w(TAG, "Screen capture visibility changed: $isVisible")
        }

        override fun onStop() {
            Log.i(TAG, "Screen capture stopped")
        }
    }, null)

    val surface = NativeFuncs.createEncoderSurface(bounds.width(), bounds.height())
    val virtualDisplay = mediaProjection.createVirtualDisplay(
        "DirectScreenShare-Capture",
        bounds.width(), bounds.height(), dpi,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        surface,
        object: VirtualDisplay.Callback() {
            override fun onPaused() {
                Log.i(TAG, "Paused")
            }

            override fun onResumed() {
                Log.i(TAG, "Resumed")
            }

            override fun onStopped() {
                Log.i(TAG, "Stopped")
            }
        },
        Handler(Looper.getMainLooper())
    )
    if(virtualDisplay==null){
        Log.e(TAG, "Failed to create virtual display")
    }

    return virtualDisplay
}

object NativeFuncs {
    init {
        System.loadLibrary("tutorial-1")
    }

    external fun createEncoderSurface(width: Int, height: Int): Surface
}


class ScreenCaptureService : Service() {
    private var virtualDisplay: VirtualDisplay? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val req_record_result = intent.getParcelableExtra<ActivityResult>("result")!!
        val bounds = intent.getParcelableExtra<Rect>("bounds")!!
        val dpi = intent.getIntExtra("dpi", 100)

        // Create a foreground notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val channel = NotificationChannel(
                "screen_capture_channel",
                "Screen Capture",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, "screen_capture_channel")
                .setContentTitle("Screen sharing active")
                .build()

            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        }


        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val mediaProjection = projectionManager.getMediaProjection(req_record_result.resultCode, req_record_result.data!! /* null-checked before starting this service */ )

        if(mediaProjection==null) Log.e(TAG, "Failed to get mediaProjection")

        virtualDisplay = setupScreenCapture(bounds, dpi, mediaProjection!!, windowManager)

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

