// whisper-jni.cpp
// JNI wrapper para whisper.cpp

#include <jni.h>
#include <string>
#include <vector>
#include "whisper.h"

extern "C" {

// Inicializar modelo Whisper
JNIEXPORT jlong JNICALL
Java_com_videocontextbot_processor_whisper_WhisperCppWrapper_initModel(
    JNIEnv* env,
    jobject thiz,
    jstring modelPath
) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    
    whisper_context_params params = whisper_context_default_params();
    params.use_gpu = false;  // CPU-only para compatibilidad
    
    whisper_context* ctx = whisper_init_from_file(path, params);
    
    env->ReleaseStringUTFChars(modelPath, path);
    
    if (ctx == nullptr) {
        env->ThrowNew(
            env->FindClass("java/lang/IllegalStateException"),
            "Failed to initialize Whisper model"
        );
        return 0;
    }
    
    return reinterpret_cast<jlong>(ctx);
}

// Transcribir audio
JNIEXPORT jobject JNICALL
Java_com_videocontextbot_processor_whisper_WhisperCppWrapper_transcribe(
    JNIEnv* env,
    jobject thiz,
    jstring audioPath,
    jlong pointer
) {
    whisper_context* ctx = reinterpret_cast<whisper_context*>(pointer);
    
    if (ctx == nullptr) {
        env->ThrowNew(
            env->FindClass("java/lang/IllegalStateException"),
            "Whisper context not initialized"
        );
        return nullptr;
    }
    
    const char* audio_file = env->GetStringUTFChars(audioPath, nullptr);
    
    // Parámetros de transcripción
    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress = false;
    wparams.print_special = false;
    wparams.print_realtime = false;
    wparams.print_timestamps = true;
    wparams.translate = false;
    wparams.n_threads = 4;  // Usar múltiples threads
    wparams.offset_ms = 0;
    wparams.duration_ms = 0;
    
    // Ejecutar transcripción
    int result = whisper_full(ctx, wparams, audio_file);
    
    env->ReleaseStringUTFChars(audioPath, audio_file);
    
    if (result != 0) {
        env->ThrowNew(
            env->FindClass("java/lang/RuntimeException"),
            "Whisper transcription failed"
        );
        return nullptr;
    }
    
    // Construir objeto TranscriptionResult
    jclass resultClass = env->FindClass("com/videocontextbot/domain/model/TranscriptionSegment");
    jmethodID segmentCtor = env->GetMethodID(resultClass, "<init>", "(FFLjava/lang/String;)V");
    
    jobjectArrayList = env->NewObjectArrayList();
    
    const int n_segments = whisper_full_n_segments(ctx);
    
    std::string fullText;
    std::string detectedLanguage;
    
    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        const int64_t start_ms = whisper_full_get_segment_t0(ctx, i) * 10;  // a segundos
        const int64_t end_ms = whisper_full_get_segment_t1(ctx, i) * 10;
        
        // Crear segmento
        jstring jText = env->NewStringUTF(text);
        jobject segment = env->NewObject(
            resultClass,
            segmentCtor,
            (float)start_ms / 1000.0f,
            (float)end_ms / 1000.0f,
            jText
        );
        
        env->DeleteLocalRef(jText);
        env->CallVoidMethod(arrayList, addMethod, segment);
        env->DeleteLocalRef(segment);
        
        // Acumular texto completo
        if (i > 0) fullText += " ";
        fullText += text;
        
        // Detectar idioma (primer segmento)
        if (i == 0) {
            detectedLanguage = whisper_lang_str(whisper_full_lang_id(ctx));
        }
    }
    
    // Crear TranscriptionResult
    jclass transcriptionClass = env->FindClass("com/videocontextbot/processor/transcription/TranscriptionResult");
    jmethodID transcriptionCtor = env->GetMethodID(
        transcriptionClass,
        "<init>",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;)V"
    );
    
    jstring jFullText = env->NewStringUTF(fullText.c_str());
    jstring jLanguage = env->NewStringUTF(detectedLanguage.c_str());
    
    jobject transcriptionResult = env->NewObject(
        transcriptionClass,
        transcriptionCtor,
        jFullText,
        jLanguage,
        arrayList
    );
    
    // Cleanup
    env->DeleteLocalRef(jFullText);
    env->DeleteLocalRef(jLanguage);
    
    return transcriptionResult;
}

// Liberar modelo
JNIEXPORT void JNICALL
Java_com_videocontextbot_processor_whisper_WhisperCppWrapper_freeModel(
    JNIEnv* env,
    jobject thiz,
    jlong pointer
) {
    whisper_context* ctx = reinterpret_cast<whisper_context*>(pointer);
    
    if (ctx != nullptr) {
        whisper_free(ctx);
    }
}

} // extern "C"
