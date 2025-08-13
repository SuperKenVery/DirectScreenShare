//
// Created by ken on 8/13/25.
//

#define LOG_TAG "NativeViewScreen"

#include <gst/gst.h>
#include <android/log_macros.h>
#include <glib.h>
#include <android/native_window.h>
#include <gst/video/videooverlay.h>

void startViewingScreen(ANativeWindow *surface){
    GError *error = nullptr;
    auto pipeline = gst_parse_launch(
        "udpsrc uri=udp://224.1.133.56:5000 caps=\"application/x-rtp, media=video, encoding-name=H265\" !"
        "rtph265depay ! "
        "h265parse ! "
        "amcvideodec ! "
        "videoconvert ! "
        "autovideosink",
        &error
    );
    if(error){
        ALOGE("Failed to launch view pipeline: %s", error->message);
        return;
    }

    auto res = gst_element_set_state(pipeline, GST_STATE_READY);
    if(res!=GST_STATE_CHANGE_SUCCESS){
        ALOGE("Failed to set view pipeline to ready state");
        return;
    }

    auto videosink = gst_bin_get_by_interface(GST_BIN(pipeline), GST_TYPE_VIDEO_OVERLAY);
    if(!videosink) {
        ALOGE("Failed to get videosink from pipeline");
        return;
    }

    gst_video_overlay_set_window_handle(GST_VIDEO_OVERLAY(videosink), (guintptr)surface);

    res = gst_element_set_state(pipeline, GST_STATE_PLAYING);
    if(res!=GST_STATE_CHANGE_SUCCESS){
        ALOGE("Failed to set view pipeline to playing");
        return;
    }

    ALOGI("Done setting up viewing pipeline");
}
