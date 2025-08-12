LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := tutorial-1
LOCAL_SRC_FILES := dummy.cpp encode.cpp gstreamer_helpers.cpp entry.cpp gstreamer_plugins.c
LOCAL_SHARED_LIBRARIES := gstreamer_android
LOCAL_LDLIBS := -llog -lmediandk -landroid
include $(BUILD_SHARED_LIBRARY)

override GSTREAMER_ROOT_ANDROID := /home/ken/AndroidStudioProjects/DirectScreenShare-worktrees/DirectScreenShare/app/src/main/cpp/gstreamer_libs
#override GSTREAMER_ROOT_ANDROID := /home/ken/Downloads/gstreamer-1.0-android-universal-1.20.7

ifndef GSTREAMER_ROOT_ANDROID
$(error GSTREAMER_ROOT_ANDROID is not defined!)
endif

ifeq ($(TARGET_ARCH_ABI),armeabi)
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)/arm
else ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)/armv7
else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)/arm64
else ifeq ($(TARGET_ARCH_ABI),x86)
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)/x86
else ifeq ($(TARGET_ARCH_ABI),x86_64)
GSTREAMER_ROOT        := $(GSTREAMER_ROOT_ANDROID)/x86_64
else
$(error Target arch ABI not supported: $(TARGET_ARCH_ABI))
endif

GSTREAMER_NDK_BUILD_PATH  := $(GSTREAMER_ROOT)/share/gst-android/ndk-build/
GSTREAMER_PLUGINS         := libav videoparsersbad opengl rtp rtpmanager coreelements app vpx x264 x265 videofilter opus autodetect audioconvert audioresample audiorate coreelements udp videotestsrc
GSTREAMER_EXTRA_DEPS      := gstreamer-plugins-base-1.0 gstreamer-rtp-1.0 gmodule-2.0 glib-2.0 gstreamer-app-1.0 gstreamer-video-1.0
GSTREAMER_EXTRA_LIBS      := -liconv
include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer-1.0.mk


# $(error trying to include $(GSTREAMER_NDK_BUILD_PATH)/gstreamer-1.0.mk)
