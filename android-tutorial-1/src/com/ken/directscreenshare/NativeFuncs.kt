package com.ken.directscreenshare

import android.view.Surface


object NativeFuncs {
    init {
        System.loadLibrary("tutorial-1")
        initializeGstreamer()
    }

    external fun initializeGstreamer()

    external fun createEncoderSurface(width: Int, height: Int): Surface

    external fun startViewingScreen(surface: Surface)
}