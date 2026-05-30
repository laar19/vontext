package com.videocontextbot.processor.whisper

import com.videocontextbot.domain.model.TranscriptionSegment

class WhisperCppWrapper {
    init {
        System.loadLibrary("whisper-jni")
    }

    external fun initModel(modelPath: String): Long

    external fun transcribe(audioPath: String, pointer: Long): TranscriptionResult

    external fun freeModel(pointer: Long)

    companion object {
        init {
            System.loadLibrary("whisper-jni")
        }
    }
}
