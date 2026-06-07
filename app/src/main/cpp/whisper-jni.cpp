#include "whisper.h"
#include <jni.h>
#include <string>
#include <vector>
#include <fstream>
#include <cstdint>

extern "C" {

static std::vector<float> loadWavFile(const char* filename) {
    std::ifstream file(filename, std::ios::binary);
    if (!file) {
        return {};
    }

    char header[44];
    file.read(header, 44);
    
    if (header[0] != 'R' || header[1] != 'I' || header[2] != 'F' || header[3] != 'F') {
        return {};
    }

    int bitsPerSample = header[34];
    int audioFormat = header[22];
    
    if (audioFormat != 1 && audioFormat != 3) {
        return {};
    }

    std::vector<int16_t> samples16;
    if (bitsPerSample == 16) {
        int16_t sample;
        while (file.read(reinterpret_cast<char*>(&sample), 2)) {
            samples16.push_back(sample);
        }
        
        std::vector<float> samples;
        samples.reserve(samples16.size());
        for (int16_t s : samples16) {
            samples.push_back(s / 32768.0f);
        }
        return samples;
    } else if (bitsPerSample == 32) {
        std::vector<float> samples;
        float sample;
        while (file.read(reinterpret_cast<char*>(&sample), 4)) {
            samples.push_back(sample);
        }
        return samples;
    }

    return {};
}

JNIEXPORT jlong JNICALL
Java_com_videocontextbot_processor_whisper_WhisperCppWrapper_initModel(
    JNIEnv* env,
    jobject thiz,
    jstring modelPath
) {
    const char* path = env->GetStringUTFChars(modelPath, nullptr);
    if (path == nullptr) {
        return 0;
    }

    struct whisper_context_params params = whisper_context_default_params();
    whisper_context* ctx = whisper_init_from_file_with_params(path, params);

    env->ReleaseStringUTFChars(modelPath, path);

    return reinterpret_cast<jlong>(ctx);
}

JNIEXPORT jobject JNICALL
Java_com_videocontextbot_processor_whisper_WhisperCppWrapper_transcribe(
    JNIEnv* env,
    jobject thiz,
    jstring audioPath,
    jlong pointer
) {
    whisper_context* ctx = reinterpret_cast<whisper_context*>(pointer);
    if (ctx == nullptr) {
        return nullptr;
    }

    const char* path = env->GetStringUTFChars(audioPath, nullptr);
    if (path == nullptr) {
        return nullptr;
    }

    std::vector<float> samples = loadWavFile(path);
    env->ReleaseStringUTFChars(audioPath, path);

    if (samples.empty()) {
        return nullptr;
    }

    struct whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_realtime = false;
    wparams.print_progress = false;
    wparams.print_timestamps = false;
    wparams.print_special = false;
    wparams.translate = false;
    wparams.language = nullptr;
    wparams.n_threads = 4;
    wparams.offset_ms = 0;
    wparams.no_context = true;
    wparams.single_segment = false;

    int resultCode = whisper_full(ctx, wparams, samples.data(), static_cast<int>(samples.size()));
    if (resultCode != 0) {
        return nullptr;
    }

    const int n_segments = whisper_full_n_segments(ctx);
    std::string full_text;
    std::string detected_language = whisper_lang_str(whisper_full_lang_id(ctx));

    jclass resultClass = env->FindClass("com/videocontextbot/processor/whisper/TranscriptionResult");
    jclass segmentClass = env->FindClass("com/videocontextbot/domain/model/TranscriptionSegment");
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListInit = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");

    jobject segmentList = env->NewObject(arrayListClass, arrayListInit);

    for (int i = 0; i < n_segments; ++i) {
        const char* text = whisper_full_get_segment_text(ctx, i);
        const int64_t t0 = whisper_full_get_segment_t0(ctx, i);
        const int64_t t1 = whisper_full_get_segment_t1(ctx, i);

        if (i > 0) full_text += " ";
        full_text += text;

        jmethodID segmentInit = env->GetMethodID(
            segmentClass, "<init>", "(FFLjava/lang/String;)V"
        );
        jstring segmentText = env->NewStringUTF(text);
        jobject segment = env->NewObject(
            segmentClass, segmentInit,
            static_cast<float>(t0) / 1000.0f,
            static_cast<float>(t1) / 1000.0f,
            segmentText
        );
        env->CallBooleanMethod(segmentList, arrayListAdd, segment);
        env->DeleteLocalRef(segmentText);
        env->DeleteLocalRef(segment);
    }

    jmethodID resultInit = env->GetMethodID(
        resultClass, "<init>",
        "(Ljava/lang/String;Ljava/lang/String;Ljava/util/List;)V"
    );
    jstring resultText = env->NewStringUTF(full_text.c_str());
    jstring resultLang = env->NewStringUTF(detected_language.c_str());

    jobject resultObj = env->NewObject(
        resultClass, resultInit,
        resultText, resultLang, segmentList
    );

    env->DeleteLocalRef(resultText);
    env->DeleteLocalRef(resultLang);
    env->DeleteLocalRef(segmentList);

    return resultObj;
}

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
