// JNI-мост к стабильному C ABI диаризации NeMo-Speech.cpp (include/nemo_speech/diar.h).
// Kotlin-сторона: ru.nuclearhack.stenographer.core.diarization.NativeDiarizer.
//
// Ошибки C API превращаются в IllegalStateException с текстом nemo_speech_asr_last_error().
// Хэндлы model/stream — указатели, упакованные в jlong; владеет ими Kotlin-код (create/destroy в finally).

#include <jni.h>
#include <android/log.h>

#include <cstdint>
#include <string>
#include <vector>

#include "nemo_speech/diar.h"

namespace {

constexpr const char* kTag = "StenoDiar";

void ThrowIllegalState(JNIEnv* env, const std::string& message) {
    __android_log_print(ANDROID_LOG_ERROR, kTag, "%s", message.c_str());
    if (env->ExceptionCheck()) return;
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    if (cls != nullptr) env->ThrowNew(cls, message.c_str());
}

bool Check(JNIEnv* env, nemo_speech_asr_status status, const char* call) {
    if (status == NEMO_SPEECH_ASR_OK) return true;
    const char* detail = nemo_speech_asr_last_error();
    ThrowIllegalState(env, std::string(call) + " failed (status " + std::to_string(status) + "): " +
                               (detail != nullptr ? detail : ""));
    return false;
}

/** RAII над GetStringUTFChars. */
class JStringChars {
  public:
    JStringChars(JNIEnv* env, jstring value)
        : env_(env), value_(value), chars_(value != nullptr ? env->GetStringUTFChars(value, nullptr) : nullptr) {}
    ~JStringChars() {
        if (chars_ != nullptr) env_->ReleaseStringUTFChars(value_, chars_);
    }
    JStringChars(const JStringChars&) = delete;
    JStringChars& operator=(const JStringChars&) = delete;

    const char* get() const { return chars_; }

  private:
    JNIEnv* env_;
    jstring value_;
    const char* chars_;
};

template <typename T>
T* FromHandle(jlong handle) {
    return reinterpret_cast<T*>(static_cast<intptr_t>(handle));
}

jlong ToHandle(const void* pointer) {
    return static_cast<jlong>(reinterpret_cast<intptr_t>(pointer));
}

}  // namespace

#define STENO_JNI(name) Java_ru_nuclearhack_stenographer_core_diarization_NativeDiarizer_##name

extern "C" {

JNIEXPORT jlong JNICALL STENO_JNI(nativeCreate)(JNIEnv* env, jclass, jstring model_path, jstring preset) {
    JStringChars path(env, model_path);
    JStringChars preset_name(env, preset);

    nemo_speech_diar_model_config cfg{};
    cfg.size = sizeof(cfg);
    cfg.model_path = path.get();
    cfg.gpu = -1;                   // только CPU
    cfg.preset = preset_name.get();
    cfg.left_context_frames = -1;   // < 0 — оставить значение пресета (0 — валидное явное значение)

    nemo_speech_diar_model* model = nullptr;
    if (!Check(env, nemo_speech_diar_create(&cfg, &model), "nemo_speech_diar_create")) return 0;

    __android_log_print(ANDROID_LOG_INFO, kTag, "model loaded: preset=%s, speakers=%d, %.3f s/frame",
                        preset_name.get(), nemo_speech_diar_num_speakers(model),
                        nemo_speech_diar_seconds_per_frame(model));
    return ToHandle(model);
}

JNIEXPORT void JNICALL STENO_JNI(nativeDestroy)(JNIEnv*, jclass, jlong model) {
    nemo_speech_diar_destroy(FromHandle<nemo_speech_diar_model>(model));
}

JNIEXPORT jlong JNICALL STENO_JNI(nativeStreamOpen)(JNIEnv* env, jclass, jlong model) {
    nemo_speech_diar_stream* stream = nullptr;
    if (!Check(env, nemo_speech_diar_stream_open(FromHandle<nemo_speech_diar_model>(model), &stream),
               "nemo_speech_diar_stream_open")) {
        return 0;
    }
    return ToHandle(stream);
}

JNIEXPORT void JNICALL STENO_JNI(nativeStreamPush)(JNIEnv* env, jclass, jlong stream, jfloatArray samples,
                                                  jint offset, jint count, jint sample_rate) {
    if (count <= 0) return;
    // Копия, а не GetPrimitiveArrayCritical: push может считать модель долго, а critical-секция блокирует GC.
    std::vector<float> buffer(static_cast<size_t>(count));
    env->GetFloatArrayRegion(samples, offset, count, buffer.data());
    if (env->ExceptionCheck()) return;
    Check(env,
          nemo_speech_diar_stream_push_f32(FromHandle<nemo_speech_diar_stream>(stream), buffer.data(),
                                           buffer.size(), sample_rate),
          "nemo_speech_diar_stream_push_f32");
}

JNIEXPORT void JNICALL STENO_JNI(nativeStreamFinish)(JNIEnv* env, jclass, jlong stream) {
    Check(env, nemo_speech_diar_stream_finish(FromHandle<nemo_speech_diar_stream>(stream)),
          "nemo_speech_diar_stream_finish");
}

/** Сегменты плоским массивом: [start_sec, end_sec, speaker (с единицы)] × N. Нули — значения библиотеки. */
JNIEXPORT jdoubleArray JNICALL STENO_JNI(nativeStreamSegments)(JNIEnv* env, jclass, jlong stream, jfloat onset,
                                                              jfloat offset, jdouble pad_onset_sec,
                                                              jdouble pad_offset_sec, jdouble min_gap_sec,
                                                              jdouble min_duration_sec) {
    nemo_speech_diar_segmentation_config cfg{};
    cfg.size = sizeof(cfg);
    cfg.onset = onset;
    cfg.offset = offset;
    cfg.pad_onset_sec = pad_onset_sec;
    cfg.pad_offset_sec = pad_offset_sec;
    cfg.min_gap_sec = min_gap_sec;
    cfg.min_duration_sec = min_duration_sec;

    const auto* handle = FromHandle<nemo_speech_diar_stream>(stream);
    size_t count = 0;
    if (!Check(env, nemo_speech_diar_segments(handle, &cfg, nullptr, 0, &count), "nemo_speech_diar_segments")) {
        return nullptr;
    }
    std::vector<nemo_speech_diar_segment> segments(count);
    if (count > 0 &&
        !Check(env, nemo_speech_diar_segments(handle, &cfg, segments.data(), segments.size(), &count),
               "nemo_speech_diar_segments")) {
        return nullptr;
    }

    std::vector<jdouble> flat;
    flat.reserve(count * 3);
    for (size_t i = 0; i < count; ++i) {
        flat.push_back(segments[i].start_time);
        flat.push_back(segments[i].end_time);
        flat.push_back(static_cast<jdouble>(segments[i].speaker));
    }
    jdoubleArray result = env->NewDoubleArray(static_cast<jsize>(flat.size()));
    if (result == nullptr) return nullptr;
    env->SetDoubleArrayRegion(result, 0, static_cast<jsize>(flat.size()), flat.data());
    return result;
}

JNIEXPORT void JNICALL STENO_JNI(nativeStreamClose)(JNIEnv*, jclass, jlong stream) {
    nemo_speech_diar_stream_close(FromHandle<nemo_speech_diar_stream>(stream));
}

}  // extern "C"
