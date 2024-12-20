package com.mj.scorecounterrc.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mj.scorecounterrc.Constants


fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) ==
            PackageManager.PERMISSION_GRANTED

fun Context.hasBtPermissions(): Boolean =
    hasPermission(Manifest.permission.BLUETOOTH_SCAN)
            && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Context.hasNotificationsPermission(): Boolean =
    hasPermission(Manifest.permission.POST_NOTIFICATIONS)

fun Context.requestBtPermissions(activity: Activity) {
    ActivityCompat.requestPermissions(activity,
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        ),
        Constants.BT_PERMISSIONS_REQUEST_CODE
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun requestNotificationsPermission(activity: Activity) {
    ActivityCompat.requestPermissions(activity,
        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
        Constants.NOTIFICATIONS_PERMISSIONS_REQUEST_CODE
    )
}