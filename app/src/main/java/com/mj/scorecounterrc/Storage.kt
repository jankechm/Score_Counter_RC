package com.mj.scorecounterrc

import android.content.Context

class Storage(private val context: Context) {

    companion object {
        const val PREFS_NAME = "Prefs"
        const val PREF_LAST_DEVICE_ADDRESS = "lastDeviceAddress"
        const val PREF_SCORE_1 = "score1"
        const val PREF_SCORE_2 = "score2"
        const val PREF_TIMESTAMP = "timestamp"
        const val PREF_SC_FACES_TO_REFEREE = "sc_faces_to_referee"
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

    fun saveSCOrientation(scFacesToReferee: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(PREF_SC_FACES_TO_REFEREE, scFacesToReferee)
            apply()
        }
    }

    /**
     * @return true if stored Score Counter orientation was: facing to the referee.
     */
    fun getSCOrientation(): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(PREF_SC_FACES_TO_REFEREE, false)
    }

}