package com.vontext.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.vontext.data.local.dao.JobDao
import com.vontext.domain.model.Job

@Database(entities = [Job::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
}
