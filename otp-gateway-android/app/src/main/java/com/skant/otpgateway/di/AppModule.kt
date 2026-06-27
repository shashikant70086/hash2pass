package com.skant.otpgateway.di

import android.content.Context
import androidx.room.Room
import com.skant.otpgateway.data.local.GatewayDb
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun db(@ApplicationContext ctx: Context): GatewayDb =
        Room.databaseBuilder(ctx, GatewayDb::class.java, "gateway.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun sendLogDao(db: GatewayDb) = db.sendLogDao()
    @Provides fun otpPendingDao(db: GatewayDb) = db.otpPendingDao()
}
