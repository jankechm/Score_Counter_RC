package com.mj.scorecounterrc.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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

fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("No activity")
}