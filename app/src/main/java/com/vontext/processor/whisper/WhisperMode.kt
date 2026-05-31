package com.vontext.processor.whisper

enum class WhisperMode {
    LOCAL_TINY,
    LOCAL_BASE,
    LOCAL_SMALL,
    LOCAL_MEDIUM,
    REMOTE_OPENAI,
    REMOTE_GROQ,
    REMOTE_DEEPSEEK,
    REMOTE_OLLAMA
}

enum class WhisperModel(val filename: String, val sizeMb: Int) {
    TINY("ggml-tiny", 75),
    BASE("ggml-base", 142),
    SMALL("ggml-small", 466),
    MEDIUM("ggml-medium", 1537),
    LARGE_V3("ggml-large-v3", 3093)
}
