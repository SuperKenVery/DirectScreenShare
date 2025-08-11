//
// Created by ken on 8/10/25.
//

#ifndef DIRECTSCREENSHARE_GSTREAMER_HELPERS_H
#define DIRECTSCREENSHARE_GSTREAMER_HELPERS_H

#include <gst/gst.h>
#include <gst/app/gstappsrc.h>

struct GstreamerHelperState {
    GstElement *pipeline;
    GstElement *appsrc;
    GMainLoop *loop;
    pthread_t gstreamer_thread;

    int width, height;

    GstreamerHelperState(int width, int height);
};

void start_gstreamer_multicast(GstreamerHelperState *ctx);
void push_buffer_to_gstreamer(GstreamerHelperState *ctx, uint8_t* data, size_t size, int64_t pts);
void stop_gstreamer_multicast(GstreamerHelperState *ctx);

#endif //DIRECTSCREENSHARE_GSTREAMER_HELPERS_H
