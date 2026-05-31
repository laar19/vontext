package com.vontext.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey val jobId: String,
    val status: JobStatus,
    val videoPath: String,
    val videoFilename: String,
    val hasAudio: Boolean?,
    val videoDuration: Float?,
    val additionalNotes: String?,
    val progress: Int,
    val progressMessage: String,
    val errorMessage: String?,
    val createdAt: Long,
    val startedAt: Long?,
    val completedAt: Long?,
    val outputFolder: String?,
    val pdfPath: String?,
    val zipPath: String?,
    val frameInterval: Int?,
    val whisperMode: String?,
    val transcriptionSegments: Int?
)

enum class JobStatus {
    PENDING,
    ANALYZING,
    EXTRACTING_FRAMES,
    TRANSCRIBING,
    GENERATING_PDF,
    CREATING_ZIP,
    COMPLETED,
    FAILED,
    CANCELLED
}
