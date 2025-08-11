package com.ken.directscreenshare

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.app.ActivityCompat


fun hasPerm(context: Context, perm: String, atLeastApiLevel: Int = 0): Boolean {
    if(Build.VERSION.SDK_INT >= atLeastApiLevel)
        return ActivityCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    else
        return true;
}

fun checkPermissions(activity: Activity): Boolean {
    val fine_location = hasPerm(activity, Manifest.permission.ACCESS_FINE_LOCATION)
    val nearby_wifi_dev = hasPerm(activity, Manifest.permission.NEARBY_WIFI_DEVICES, 33)

    if (!(fine_location && nearby_wifi_dev)) {
        Toast.makeText(activity, "No permission, requesting...", Toast.LENGTH_SHORT).show()
        ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.NEARBY_WIFI_DEVICES), 100)
        return false
    }
    return true
}