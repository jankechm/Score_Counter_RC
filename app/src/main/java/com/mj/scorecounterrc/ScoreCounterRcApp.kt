package com.mj.scorecounterrc

import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.content.IntentFilter
import android.os.Build
import com.mj.scorecounterrc.broadcastreceiver.BtStateChangedReceiver
import com.mj.scorecounterrc.smartwatch.SmartwatchManager
import dagger.hilt.android.HiltAndroidApp
import fr.bipi.treessence.file.FileLoggerTree
import timber.log.Timber
import java.util.logging.FileHandler
import java.util.logging.Logger

@HiltAndroidApp
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                BtStateChangedReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(
                BtStateChangedReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            )
        }
    }
}