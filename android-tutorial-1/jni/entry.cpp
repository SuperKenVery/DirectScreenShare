#include <android/native_window_jni.h>
#include <jni.h>
#include <gst/gst.h>

#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

ANativeWindow* create_encoder_surface(int width, int height);
void startViewingScreen(ANativeWindow *surface);
extern "C" void gst_android_register_static_plugins(void);

extern "C"
JNIEXPORT jobject JNICALL
Java_com_ken_directscreenshare_NativeFuncs_createEncoderSurface(JNIEnv *env, jobject thiz,
                                                                    jint width, jint height) {
    auto native_window = create_encoder_surface(width, height);
    auto surface = ANativeWindow_toSurface(env, native_window);
    return surface;
}



extern "C"
JNIEXPORT void JNICALL
Java_com_ken_directscreenshare_NativeFuncs_initializeGstreamer(JNIEnv *env, jobject thiz) {
    gst_android_register_static_plugins();
    gst_init(nullptr, nullptr);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_ken_directscreenshare_NativeFuncs_startViewingScreen(JNIEnv *env, jobject thiz,
                                                              jobject surface) {
    auto native_window = ANativeWindow_fromSurface(env, surface);
    startViewingScreen(native_window);
}