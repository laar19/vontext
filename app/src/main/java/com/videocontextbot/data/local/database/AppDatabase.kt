package com.videocontextbot.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.videocontextbot.data.local.dao.JobDao
import com.videocontextbot.domain.model.Job

@Database(entities = [Job::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
}
