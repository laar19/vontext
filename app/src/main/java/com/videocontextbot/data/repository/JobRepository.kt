package com.videocontextbot.data.repository

import com.videocontextbot.data.local.dao.JobDao
import com.videocontextbot.domain.model.Job
import com.videocontextbot.domain.model.JobStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JobRepository @Inject constructor(
    private val jobDao: JobDao
) {
    fun getAllJobs(): Flow<List<Job>> = jobDao.getAllJobs()

    fun getJobById(jobId: String): Flow<Job?> = jobDao.getJobById(jobId)

    suspend fun getJobByIdOnce(jobId: String): Job? = jobDao.getJobByIdOnce(jobId)

    suspend fun insertJob(job: Job) = jobDao.insertJob(job)

    suspend fun updateProgress(jobId: String, progress: Int, message: String) {
        jobDao.updateProgress(jobId, progress, message)
    }

    suspend fun updateStatus(jobId: String, status: JobStatus) {
        jobDao.updateStatus(jobId, status)
    }

    suspend fun updateError(jobId: String, status: JobStatus, error: String?) {
        jobDao.updateError(jobId, status, error)
    }

    suspend fun deleteJob(jobId: String) = jobDao.deleteJob(jobId)
}
