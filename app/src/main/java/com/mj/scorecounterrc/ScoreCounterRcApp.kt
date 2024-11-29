package com.mj.scorecounterrc

import android.Manifest
import android.app.Activity
import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mj.scorecounterrc.data.manager.StorageManager
import com.mj.scorecounterrc.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.smartwatch.SmartwatchManager
import fr.bipi.treessence.file.FileLoggerTree
import timber.log.Timber
import java.util.logging.FileHandler
import java.util.logging.Logger

class ScoreCounterRcApp : Application() {

    class MyFileLoggerTree(logger: Logger, fileHandler: FileHandler?, path: String, nbFiles: Int,
                           priority: Int
    ) : FileLoggerTree(logger, fileHandler, path, nbFiles, priority
    ) {
        override fun createStackElementTag(element: StackTraceElement): String {
            return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
        }
    }


    override fun onCreate() {
        super.onCreate()

        ScoreCounterConnectionManager.app = this
        SmartwatchManager.app = this
        StorageManager.app = this

        ScoreCounterConnectionManager.btAdapter = getSystemService(BluetoothManager::class.java)
            ?.adapter

        if (BuildConfig.DEBUG) {
            Timber.plant(object : Timber.DebugTree() {
                override fun createStackElementTag(element: StackTraceElement): String {
                    return "(${element.fileName}:${element.lineNumber})#${element.methodName}"
                }
            })

//            val logsDir = getExternalFilesDir(null).toString() + File.separator + "logs"
//
//            val flt = FileLoggerTree.Builder()
//                .withFileName("score_counter_%g.log")
//                .withDirName(logsDir)
//                .withSizeLimit(5_242_880)
//                .withFileLimit(5)
//                .withMinPriority(Log.DEBUG)
//                .appendToFile(true)
//                .build();
//
//            Timber.plant(flt)
        }

        SmartwatchManager.registerReceivers()
        SmartwatchManager.registerListeners()

        ScoreCounterConnectionManager.registerReceivers()
        ScoreCounterConnectionManager.registerListeners()

        SmartwatchManager.startSmartwatchApp()
    }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED

    fun hasBtPermissions(): Boolean =
        hasPermission(Manifest.permission.BLUETOOTH_SCAN)
                && hasPermission(Manifest.permission.BLUETOOTH_CONNECT)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun hasNotificationsPermission(): Boolean =
        hasPermission(Manifest.permission.POST_NOTIFICATIONS)

    fun requestBtPermissions(activity: Activity) {
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
            Constants.NOTIFICATIONS_PERMISSIONS_REQUEST_CODE)
    }
}