// JNI-мост к llama.cpp: загрузка GGUF, жадная генерация по готовому промпту, освобождение.
// Промпт (шаблон чата LFM3) собирает Kotlin; здесь только токенизация, prefill и декодирование.
#include <jni.h>
#include <android/log.h>

#include <string>
#include <vector>

#include "llama.h"

#define LOG_TAG "steno_llm"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct Session {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    const llama_vocab *vocab = nullptr;
};

void throwIllegalState(JNIEnv *env, const std::string &msg) {
    jclass cls = env->FindClass("java/lang/IllegalStateException");
    env->ThrowNew(cls, msg.c_str());
}

void logCallback(ggml_log_level level, const char *text, void *) {
    if (level >= GGML_LOG_LEVEL_ERROR) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s", text);
}

std::string jstringToUtf8(JNIEnv *env, jstring s) {
    // GetStringUTFChars отдаёт «modified UTF-8»; для русского текста без суррогатов совпадает с UTF-8,
    // но надёжнее взять байты через String.getBytes("UTF-8").
    jclass strCls = env->GetObjectClass(s);
    jmethodID getBytes = env->GetMethodID(strCls, "getBytes", "(Ljava/lang/String;)[B");
    jstring charset = env->NewStringUTF("UTF-8");
    auto bytes = (jbyteArray) env->CallObjectMethod(s, getBytes, charset);
    jsize len = env->GetArrayLength(bytes);
    std::string out(len, '\0');
    env->GetByteArrayRegion(bytes, 0, len, reinterpret_cast<jbyte *>(out.data()));
    env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(charset);
    return out;
}

jstring utf8ToJstring(JNIEnv *env, const std::string &s) {
    jbyteArray bytes = env->NewByteArray((jsize) s.size());
    env->SetByteArrayRegion(bytes, 0, (jsize) s.size(), reinterpret_cast<const jbyte *>(s.data()));
    jclass strCls = env->FindClass("java/lang/String");
    jmethodID ctor = env->GetMethodID(strCls, "<init>", "([BLjava/lang/String;)V");
    jstring charset = env->NewStringUTF("UTF-8");
    auto out = (jstring) env->NewObject(strCls, ctor, bytes, charset);
    env->DeleteLocalRef(bytes);
    env->DeleteLocalRef(charset);
    return out;
}

}  // namespace

extern "C" {

JNIEXPORT jlong JNICALL
Java_ru_nuclearhack_stenographer_core_llm_NativeLlm_nativeLoad(
        JNIEnv *env, jclass, jstring jpath, jint nThreads, jint nCtx) {
    static bool backendReady = false;
    if (!backendReady) {
        llama_log_set(logCallback, nullptr);
        llama_backend_init();
        backendReady = true;
    }
    std::string path = jstringToUtf8(env, jpath);

    llama_model_params mp = llama_model_default_params();
    mp.n_gpu_layers = 0;
    // Без перепаковки весов под CPU: пик памяти Q4_K_M 1.27 ГБ вместо 1.96 при потере скорости < 2 %.
    mp.use_extra_bufts = false;
    llama_model *model = llama_model_load_from_file(path.c_str(), mp);
    if (!model) {
        throwIllegalState(env, "Не удалось загрузить модель: " + path);
        return 0;
    }
    llama_context_params cp = llama_context_default_params();
    cp.n_ctx = (uint32_t) nCtx;
    cp.n_batch = 512;
    cp.n_ubatch = 512;
    cp.n_threads = nThreads;
    cp.n_threads_batch = nThreads;
    cp.no_perf = true;
    llama_context *ctx = llama_init_from_model(model, cp);
    if (!ctx) {
        llama_model_free(model);
        throwIllegalState(env, "Не удалось создать контекст llama.cpp");
        return 0;
    }
    auto *s = new Session{model, ctx, llama_model_get_vocab(model)};
    LOGI("model loaded: %s, n_ctx=%u, threads=%d", path.c_str(), llama_n_ctx(ctx), nThreads);
    return reinterpret_cast<jlong>(s);
}

JNIEXPORT void JNICALL
Java_ru_nuclearhack_stenographer_core_llm_NativeLlm_nativeFree(JNIEnv *, jclass, jlong handle) {
    auto *s = reinterpret_cast<Session *>(handle);
    if (!s) return;
    llama_free(s->ctx);
    llama_model_free(s->model);
    delete s;
}

JNIEXPORT jint JNICALL
Java_ru_nuclearhack_stenographer_core_llm_NativeLlm_nativeCountTokens(
        JNIEnv *env, jclass, jlong handle, jstring jtext) {
    auto *s = reinterpret_cast<Session *>(handle);
    std::string text = jstringToUtf8(env, jtext);
    int n = -llama_tokenize(s->vocab, text.c_str(), (int32_t) text.size(), nullptr, 0, false, true);
    return n;
}

// listener: объект с методами `boolean onPrefill(int done, int total)` и `boolean onToken(int generated)`;
// false — отмена (генерация прерывается, возвращается то, что успели).
JNIEXPORT jstring JNICALL
Java_ru_nuclearhack_stenographer_core_llm_NativeLlm_nativeGenerate(
        JNIEnv *env, jclass, jlong handle, jstring jprompt, jint maxTokens, jobject listener) {
    auto *s = reinterpret_cast<Session *>(handle);
    jclass lcls = env->GetObjectClass(listener);
    jmethodID onPrefill = env->GetMethodID(lcls, "onPrefill", "(II)Z");
    jmethodID onToken = env->GetMethodID(lcls, "onToken", "(I)Z");

    std::string prompt = jstringToUtf8(env, jprompt);
    int n = -llama_tokenize(s->vocab, prompt.c_str(), (int32_t) prompt.size(), nullptr, 0, false, true);
    std::vector<llama_token> tokens(n);
    if (llama_tokenize(s->vocab, prompt.c_str(), (int32_t) prompt.size(), tokens.data(), n, false, true) < 0) {
        throwIllegalState(env, "Ошибка токенизации промпта");
        return nullptr;
    }
    const int nCtx = (int) llama_n_ctx(s->ctx);
    if (n + 8 >= nCtx) {
        throwIllegalState(env, "Промпт длиннее контекста: " + std::to_string(n) + " токенов");
        return nullptr;
    }
    llama_memory_clear(llama_get_memory(s->ctx), true);

    const int nBatch = (int) llama_n_batch(s->ctx);
    for (int i = 0; i < n; i += nBatch) {
        int cnt = std::min(nBatch, n - i);
        llama_batch batch = llama_batch_get_one(tokens.data() + i, cnt);
        if (llama_decode(s->ctx, batch) != 0) {
            throwIllegalState(env, "llama_decode упал на prefill");
            return nullptr;
        }
        if (!env->CallBooleanMethod(listener, onPrefill, i + cnt, n)) return utf8ToJstring(env, "");
    }

    llama_sampler *smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    // Жадный выбор + штраф за повторы 1.1 на последних 64 токенах: без него модель изредка зацикливается.
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(llama_vocab_n_tokens(s->vocab), 64, 1.1f, 0.0f, 0.0f));
    llama_sampler_chain_add(smpl, llama_sampler_init_greedy());

    std::string out;
    char buf[256];
    int limit = std::min((int) maxTokens, nCtx - n - 1);
    for (int g = 0; g < limit; ++g) {
        llama_token tok = llama_sampler_sample(smpl, s->ctx, -1);
        if (llama_vocab_is_eog(s->vocab, tok)) break;
        int len = llama_token_to_piece(s->vocab, tok, buf, sizeof(buf), 0, false);
        if (len > 0) out.append(buf, len);
        llama_batch batch = llama_batch_get_one(&tok, 1);
        if (llama_decode(s->ctx, batch) != 0) break;
        if (!env->CallBooleanMethod(listener, onToken, g + 1)) break;
    }
    llama_sampler_free(smpl);
    return utf8ToJstring(env, out);
}

}  // extern "C"
