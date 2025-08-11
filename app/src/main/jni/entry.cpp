#include <android/native_window_jni.h>
#include <jni.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

ANativeWindow* create_encoder_surface(int width, int height);


extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_directscreenshare_NativeFuncs_createEncoderSurface(JNIEnv *env, jobject thiz,
                                                                    jint width, jint height) {
    auto native_window = create_encoder_surface(width, height);
    auto surface = ANativeWindow_toSurface(env, native_window);
    return surface;
}