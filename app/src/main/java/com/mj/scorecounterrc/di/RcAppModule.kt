package com.mj.scorecounterrc.di

import com.mj.scorecounterrc.ScoreSync
import com.mj.scorecounterrc.ScoreSyncImpl
import com.mj.scorecounterrc.broadcastreceiver.SCPebbleDataReceiver
import com.mj.scorecounterrc.communication.scorecounter.ScoreCounterConnectionManager
import com.mj.scorecounterrc.communication.scorecounter.ScoreCounterMessageSender
import com.mj.scorecounterrc.communication.smartwatch.manager.PebbleManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RcAppModule {

    companion object {
        @Provides
        fun providesScPebbleDataReceiver(): SCPebbleDataReceiver {
            return SCPebbleDataReceiver(PebbleManager.pebbleAppUUID)
        }
    }

    @Binds
    abstract fun bindScoreCounterMessageSender(
        scoreCounterConnectionManager: ScoreCounterConnectionManager
    ): ScoreCounterMessageSender

    @Binds
    abstract fun bindScoreSync(scoreSync: ScoreSyncImpl): ScoreSync
}