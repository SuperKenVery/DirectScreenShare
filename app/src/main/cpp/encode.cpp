//
// Created by ken on 8/9/25.
//

#include <media/NdkMediaCodec.h>
#include <android/log.h>
#include <errno.h>

#define LOG_TAG "NativeEncodeCpp"
#include <android/log_macros.h>
#include <pthread.h>
#include <stdio.h>
#include "gstreamer_helpers.h"

#define ENCODING "hevc"

struct ExtractEncodedDataCtx {
    GstreamerHelperState *helper_ctx;
    AMediaCodec *codec;

    ExtractEncodedDataCtx(int width, int height, AMediaCodec *codec) {
        this->helper_ctx = new GstreamerHelperState(width, height);
        this->codec = codec;
    }
};

void* extract_encoded_data(void* data) {
    ALOGI("Extract encoded data thread started");
    bool doneEncoding = false;
    int64_t pts_base = 0;
    ExtractEncodedDataCtx *ctx = (ExtractEncodedDataCtx *)data;

    start_gstreamer_multicast(ctx->helper_ctx);

    while (!doneEncoding) {
        AMediaCodecBufferInfo bufferInfo;
        ssize_t outputBufferId = AMediaCodec_dequeueOutputBuffer(ctx->codec, &bufferInfo, -1);

        if (outputBufferId == AMEDIACODEC_INFO_TRY_AGAIN_LATER) {
            ALOGW("No output available yet, while we specified infinite timeout");
            continue;
        } else if (outputBufferId == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
            ALOGE("Output format changed");
        } else if (outputBufferId == AMEDIACODEC_INFO_OUTPUT_BUFFERS_CHANGED) {
            ALOGE("Shouldn't see this: buffers changed");
        } else {
            size_t buf_size;
            uint8_t* outputData = AMediaCodec_getOutputBuffer(ctx->codec, outputBufferId, &buf_size);

            if (outputData != nullptr && bufferInfo.size > 0) {
//                ALOGD("Got valid encoded data, buffer idx=%zd", outputBufferId);

                auto pts = bufferInfo.presentationTimeUs * 1000;
                if(pts_base==0) pts_base = pts;
                pts = pts - pts_base;

                push_buffer_to_gstreamer(ctx->helper_ctx, outputData, buf_size, pts);
            }

            AMediaCodec_releaseOutputBuffer(ctx->codec, outputBufferId, false);
            if (bufferInfo.flags & AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM) {
                doneEncoding = true;
            }
        }

    }

    ALOGI("Encoder stopped");
    stop_gstreamer_multicast(ctx->helper_ctx);
    return nullptr;
}


ANativeWindow* create_encoder_surface(int width, int height) {
    auto format = AMediaFormat_new();
    AMediaFormat_setString(format, AMEDIAFORMAT_KEY_MIME, "video/" ENCODING);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_WIDTH, width);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_HEIGHT, height);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_FRAME_RATE, 60);
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_BIT_RATE, 8000000); // 8Mbps
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_I_FRAME_INTERVAL, 2); // key frame every 2 seconds
    AMediaFormat_setInt32(format, AMEDIAFORMAT_KEY_COLOR_FORMAT, 2130708361); // kotlin: MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface

    ALOGI("Creating codec");
    auto codec = AMediaCodec_createEncoderByType("video/" ENCODING);
    if(codec== nullptr) ALOGE("Failed to create codec");

    // Async callback has some criticil issues
    // AMediaCodec_releaseOutputBuffer stops the whole encoder and callback is no longer called

    ALOGI("Configuring codec");
    auto ret = AMediaCodec_configure(codec, format, nullptr, nullptr, AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
    if(ret!=AMEDIA_OK) ALOGE("Failed to configure codec with format. ret=%d", ret);

    ANativeWindow *surface;
    ALOGI("Creating input surface");
    ret = AMediaCodec_createInputSurface(codec, &surface);
    if(ret!=AMEDIA_OK) ALOGE("Failed to create input surface. ret=%d", ret);

    ALOGI("Starting codec");
    ret = AMediaCodec_start(codec);
    if(ret!=AMEDIA_OK) ALOGE("Failed to start codec. ret=%d", ret);

    ALOGI("Starting thread to take encoded data");
    pthread_t thread_id;
    auto ctx = new ExtractEncodedDataCtx(width, height, codec);
    auto ret2 = pthread_create(&thread_id, nullptr, &extract_encoded_data, ctx);
    if(ret2!=0) {
        perror("Creating thread");
    }

    ALOGI("Initializing gstreamer helper");


    return surface;
}
