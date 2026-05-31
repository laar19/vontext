package com.vontext.di

import android.content.Context
import androidx.room.Room
import com.vontext.data.local.dao.JobDao
import com.vontext.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "videocontextbot.db"
        ).build()
    }

    @Provides
    fun provideJobDao(database: AppDatabase): JobDao {
        return database.jobDao()
    }
}
