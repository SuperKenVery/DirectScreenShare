//
// Created by ken on 8/10/25.
//

#include "gstreamer_helpers.h"

#include <gst/gst.h>
#include <gst/app/gstappsrc.h>
#define LOG_TAG "GstreamerHelpers"
#include <android/log_macros.h>
#include <linux/in.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <arpa/inet.h>



GstreamerHelperState::GstreamerHelperState(int width, int height): width(width), height(height) {
    this->pipeline = nullptr;
    this->appsrc = nullptr;
    this->loop = nullptr;
}

// Function to run GStreamer in a separate thread
void* gst_main_thread(void* data) {
    ALOGI("gst_main_thread running");
    GstreamerHelperState *ctx = (GstreamerHelperState*) data;
    gst_init(nullptr, nullptr);

    // Create the GStreamer pipeline for multicast RTP streaming
    GError *err = nullptr;
    ctx->pipeline = gst_parse_launch(
            "appsrc name=source is-live=true "
            "caps=video/x-h265,stream-format=byte-stream,width=1920,height=1080,framerate=60/1 ! "
            "h265parse ! "           // Adds AUD, provides proper NAL units
            "rtph265pay pt=96 config-interval=1 ! "                       // Packetizes into RTP
            "udpsink host=192.168.3.158 port=5000 ", // auto-multicast=true
            &err);

    if(err){
        ALOGE("Failed to launch gstreamer: %s", err->message);
    }else{
        ALOGI("Successfully created gstreamer pipeline");
    }

    if (!ctx->pipeline) {
        ALOGE("Failed to create GStreamer pipeline");
        return nullptr;
    }

    ctx->appsrc = gst_bin_get_by_name(GST_BIN(ctx->pipeline), "source");
    if (!ctx->appsrc) {
        ALOGE("Failed to get appsrc from pipeline");
        gst_object_unref(ctx->pipeline);
        return nullptr;
    }

    auto res = gst_element_set_state(ctx->pipeline, GST_STATE_PLAYING);
    if(res!=GST_STATE_CHANGE_SUCCESS) {
        ALOGE("Failed to start pipeline, ret=%d", res);
        return nullptr;
    }

    ctx->loop = g_main_loop_new(nullptr, FALSE);
    g_main_loop_run(ctx->loop);

    ALOGE("Gstreamer mainloop ended");
    // Cleanup
    if (ctx->appsrc) gst_object_unref(ctx->appsrc);
    if (ctx->pipeline) gst_object_unref(ctx->pipeline);
    if (ctx->loop) g_main_loop_unref(ctx->loop);

    return nullptr;
}


void start_gstreamer_multicast(GstreamerHelperState *ctx) {
    int ret = pthread_create(&ctx->gstreamer_thread, nullptr, gst_main_thread, ctx);
    if (ret != 0) {
        ALOGE("Failed to create GStreamer thread: %s", strerror(ret));
    }
}

void push_buffer_to_gstreamer(GstreamerHelperState *ctx, uint8_t* data, size_t size, int64_t pts) {
    if (!ctx->appsrc) {
        ALOGE("push_buffer_to_gstreamer: ctx->appsrc is nullptr");
        return;
    }

    GstBuffer *buffer = gst_buffer_new_allocate(nullptr, size, nullptr);
    gst_buffer_fill(buffer, 0, data, size);

    // Set proper PTS for streaming
    GST_BUFFER_PTS(buffer) = pts;
    GST_BUFFER_DURATION(buffer) = GST_CLOCK_TIME_NONE;

    GstFlowReturn ret = gst_app_src_push_buffer(GST_APP_SRC(ctx->appsrc), buffer);
    if (ret != GST_FLOW_OK) {
        ALOGE("Failed to push buffer to GStreamer: %s", gst_flow_get_name(ret));
    }
}

void stop_gstreamer_multicast(GstreamerHelperState *ctx) {
    if (ctx->loop && g_main_loop_is_running(ctx->loop)) {
        g_main_loop_quit(ctx->loop);
    }

    pthread_join(ctx->gstreamer_thread, nullptr);
    ctx->appsrc = nullptr;
    ctx->pipeline = nullptr;
    ctx->loop = nullptr;
}
