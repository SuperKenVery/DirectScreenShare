//
// Created by ken on 8/9/25.
//

#include <media/NdkMediaCodec.h>
#include <android/log.h>

#define LOG_TAG "NativeEncodeCpp"
#include <android/log_macros.h>

#define ENCODING "hevc"

void codec_input_available(AMediaCodec *codec, void *userdata, int32_t index){}

void codec_output_available(AMediaCodec *codec, void *userdata, int32_t index, AMediaCodecBufferInfo *bfinfo){
    // TODO
    ALOGI("Got encoded data!");
}

void codec_format_changed(AMediaCodec *codec, void *userdata, AMediaFormat *format){}

void codec_error(AMediaCodec *codec, void *userdata, media_status_t error, int32_t actionCode, const char *detail){

}

void register_callback(AMediaCodec* codec) {
    AMediaCodecOnAsyncNotifyCallback callbacks;
}

ANativeWindow* create_encoder_surface(int width, int height) {
    auto format = AMediaFormat_new();
    AMediaFormat_setString(format, AMEDIAFORMAT_KEY_MIME, "video/" ENCODING);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_WIDTH, width);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_HEIGHT, height);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_FRAME_RATE, 60);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_BIT_RATE, 8000000); // 8Mbps
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_I_FRAME_INTERVAL, 2); // key frame every 2 seconds
//    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 2130708361); // kotlin: MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 2130708361);

    ALOGI("Creating codec");
    auto codec = AMediaCodec_createEncoderByType("video/" ENCODING);
    if(codec== nullptr) ALOGE("Failed to create codec");

    ALOGI("Configuring codec");
    auto ret = AMediaCodec_configure(codec, format, nullptr, nullptr, AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
    if(ret!=AMEDIA_OK) ALOGE("Failed to configure codec with format. ret=%d", ret);

    AMediaCodecOnAsyncNotifyCallback callback = {
            .onAsyncError = codec_error,
            .onAsyncFormatChanged = codec_format_changed,
            .onAsyncInputAvailable = codec_input_available,
            .onAsyncOutputAvailable = codec_output_available,
    };
    ALOGI("Configuring callback");
    ret = AMediaCodec_setAsyncNotifyCallback(codec, callback, nullptr);
    if(ret!=AMEDIA_OK) ALOGE("Failed to register encoder callback. ret=%d", ret);

    ANativeWindow *surface;
    ALOGI("Creating input surface");
    ret = AMediaCodec_createInputSurface(codec, &surface);
    if(ret!=AMEDIA_OK) ALOGE("Failed to create input surface. ret=%d", ret);

    return surface;
}
