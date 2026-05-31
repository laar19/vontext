package com.vontext.data.remote.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class TranscriptionResponse(
    val text: String,
    val language: String?,
    val segments: List<SegmentResponse>?
)

data class SegmentResponse(
    val start: Float,
    val end: Float,
    val text: String
)

interface WhisperApi {
    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribe(
        @Part("model") model: RequestBody,
        @Part file: MultipartBody.Part,
        @Part("response_format") format: RequestBody,
        @Part("timestamp_granularities") timestamps: RequestBody
    ): TranscriptionResponse
}
