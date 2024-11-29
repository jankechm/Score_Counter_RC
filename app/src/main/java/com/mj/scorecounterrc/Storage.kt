package com.mj.scorecounterrc

import android.content.Context

object Storage {

    const val PREFS_NAME = "Prefs"
    const val PREF_LAST_DEVICE_ADDRESS = "lastDeviceAddress"
    const val PREF_SCORE_1 = "score1"
    const val PREF_SCORE_2 = "score2"
    const val PREF_TIMESTAMP = "timestamp"
    const val PREF_SC_FACES_TO_REFEREE = "sc_faces_to_referee"

    // Should be injected at app.onCreate()
    var app: ScoreCounterRcApp? = null


    fun saveDeviceAddress(deviceAddress: String) {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString(PREF_LAST_DEVICE_ADDRESS, deviceAddress)
                apply()
            }
        }
    }

    fun getSavedDeviceAddress(): String? {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getString(PREF_LAST_DEVICE_ADDRESS, null)
        }
        return null
    }

    fun saveScore1(score1: Int) {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putInt(PREF_SCORE_1, score1)
                apply()
            }
        }
    }

    fun saveScore2(score2: Int) {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putInt(PREF_SCORE_2, score2)
                apply()
            }
        }
    }

    fun getScore1(): Int {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(PREF_SCORE_1, 0)
        }
        return 0
    }

    fun getScore2(): Int {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(PREF_SCORE_2, 0)
        }
        return 0
    }

    fun saveTimestamp(timestampSeconds: Long) {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putLong(PREF_TIMESTAMP, timestampSeconds)
                apply()
            }
        }
    }

    fun getTimestamp(): Long {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getLong(PREF_TIMESTAMP, 0L)
        }
        return 0L
    }

    fun saveSCOrientation(scFacesToReferee: Boolean) {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().apply {
                putBoolean(PREF_SC_FACES_TO_REFEREE, scFacesToReferee)
                apply()
            }
        }
    }

    /**
     * @return true if stored Score Counter orientation was: facing to the referee.
     */
    fun getSCOrientation(): Boolean {
        app?.let { app ->
            val prefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getBoolean(PREF_SC_FACES_TO_REFEREE, false)
        }
        return false
    }
}