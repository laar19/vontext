package com.videocontextbot.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.videocontextbot.domain.model.Job
import com.videocontextbot.domain.model.JobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface JobDao {
    @Query("SELECT * FROM jobs ORDER BY createdAt DESC")
    fun getAllJobs(): Flow<List<Job>>

    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    fun getJobById(jobId: String): Flow<Job?>

    @Query("SELECT * FROM jobs WHERE jobId = :jobId")
    suspend fun getJobByIdOnce(jobId: String): Job?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: Job)

    @Update
    suspend fun updateJob(job: Job)

    @Query("UPDATE jobs SET status = :status WHERE jobId = :jobId")
    suspend fun updateStatus(jobId: String, status: JobStatus)

    @Query("UPDATE jobs SET progress = :progress, progressMessage = :message WHERE jobId = :jobId")
    suspend fun updateProgress(jobId: String, progress: Int, message: String)

    @Query("UPDATE jobs SET status = :status, errorMessage = :error WHERE jobId = :jobId")
    suspend fun updateError(jobId: String, status: JobStatus, error: String?)

    @Query("DELETE FROM jobs WHERE jobId = :jobId")
    suspend fun deleteJob(jobId: String)
}
