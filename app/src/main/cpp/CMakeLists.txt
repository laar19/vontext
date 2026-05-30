cmake_minimum_required(VERSION 3.22.1)

project("VideoContextBot")

# Configurar C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Habilitar NEON para ARM (optimización)
if(${ANDROID_ABI} STREQUAL "arm64-v8a" OR ${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mfpu=neon")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -mfpu=neon")
endif()

# Whisper.cpp subdirectory
add_subdirectory(whisper)

# Librería JNI para Whisper
add_library(whisper-jni SHARED
    whisper-jni.cpp
)

# Enlazar con whisper.cpp
target_link_libraries(whisper-jni
    whisper
    android
    log
)

# Optimizaciones de release
if(CMAKE_BUILD_TYPE STREQUAL "Release")
    target_compile_definitions(whisper-jni PRIVATE NDEBUG)
    target_compile_options(whisper-jni PRIVATE -O3)
endif()
