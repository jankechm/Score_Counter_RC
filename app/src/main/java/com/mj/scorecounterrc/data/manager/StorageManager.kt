package com.mj.scorecounterrc.data.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val PREFS_NAME = "Prefs"
        const val PREF_LAST_DEVICE_ADDRESS = "lastDeviceAddress"
        const val PREF_SCORE_1 = "score1"
        const val PREF_SCORE_2 = "score2"
        const val PREF_TIMESTAMP = "timestamp"
        const val PREF_IS_SC_OPPOSITE_TO_THE_REFEREE = "sc_opposite_to_referee"
        const val PREF_AUTO_CONNECT_ON_STARTUP = "auto_connect_on_startup"
    }

    fun saveDeviceAddress(deviceAddress: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(PREF_LAST_DEVICE_ADDRESS, deviceAddress)
            apply()
        }
    }

    fun getSavedDeviceAddress(): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(PREF_LAST_DEVICE_ADDRESS, null)
    }

    fun saveScore1(score1: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(PREF_SCORE_1, score1)
            apply()
        }
    }

    fun saveScore2(score2: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(PREF_SCORE_2, score2)
            apply()
        }
    }

    fun getScore1(): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_SCORE_1, 0)
    }

    fun getScore2(): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(PREF_SCORE_2, 0)
}

    fun saveTimestamp(timestampSeconds: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putLong(PREF_TIMESTAMP, timestampSeconds)
            apply()
        }
    }

    fun getTimestamp(): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(PREF_TIMESTAMP, 0L)
    }

    fun saveScPosition(isScOppositeToTheReferee: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(PREF_IS_SC_OPPOSITE_TO_THE_REFEREE, isScOppositeToTheReferee)
            apply()
        }
    }

    /**
     * @return true if stored Score Counter position was: opposite to the referee.
     */
    fun getScPosition(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_IS_SC_OPPOSITE_TO_THE_REFEREE, false)
    }

    fun saveAutoConnectOnStartup(autoConnectOnStartup: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(PREF_AUTO_CONNECT_ON_STARTUP, autoConnectOnStartup)
            apply()
        }
    }

    fun getAutoConnectOnStartup(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_AUTO_CONNECT_ON_STARTUP, false)
    }
}