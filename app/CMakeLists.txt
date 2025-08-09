# Thanks: https://stackoverflow.com/questions/45118175/how-to-add-jni-c-c-native-code-to-existing-android-studio-project

cmake_minimum_required(VERSION 3.4.1)

add_library(
    native-lib
    SHARED
    src/main/cpp/entry.cpp
    src/main/cpp/encode.cpp
)


find_library(log-lib log)
find_library(android-lib android)
find_library(media-lib mediandk)


# === GStreamer ===
if(ANDROID)
    if(ANDROID_ABI STREQUAL "armeabi-v7a")
        set(GSTREAMER_ARCH armv7)
    elseif(ANDROID_ABI STREQUAL "arm64-v8a")
        set(GSTREAMER_ARCH arm64)
    elseif(ANDROID_ABI STREQUAL "x86")
        set(GSTREAMER_ARCH x86)
    elseif(ANDROID_ABI STREQUAL "x86_64")
        set(GSTREAMER_ARCH x86_64)
    else()
        message(FATAL_ERROR "Unsupported Android ABI: ${ANDROID_ABI}")
    endif()

    set(GSTREAMER_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/src/main/cpp/gstreamer_libs/${GSTREAMER_ARCH}")
else()
    message(WARNING "Not building for Android; GStreamer setup skipped.")
endif()

include_directories(
        ${GSTREAMER_ROOT}/include/gstreamer-1.0
        ${GSTREAMER_ROOT}/include/glib-2.0
        ${GSTREAMER_ROOT}/lib/glib-2.0/include)
#link_directories(
#        ${GSTREAMER_ROOT}/lib
#        ${GSTREAMER_ROOT}/lib/gstreamer-1.0)
SET(GST_LIBS gstreamer-1.0 gmodule-2.0 gobject-2.0 glib-2.0 iconv intl ffi)
SET(GST_PLUGINS_CORE_LIBS gstaudioresample gstaudiotestsrc gstaudioconvert)
SET(GST_PLUGINS_BASE gstautodetect gstallocators-1.0 orc-test-0.4 gstapp-1.0 gstaudio-1.0 gstfft-1.0 gstpbutils-1.0 gstriff-1.0 gstrtp-1.0 gstrtsp-1.0 gstsdp-1.0 gsttag-1.0 gstvideo-1.0 gstgl-1.0 orc-0.4 gstopensles OpenSLES gstaudio-1.0 gstbase-1.0)

find_library(TEST_GST_LIB
        NAMES gstreamer-1.0
        PATHS ${GSTREAMER_ROOT}/lib
        NO_DEFAULT_PATH
)
message(FATAL_ERROR, "Debug - Found gstreamer-1.0: ${TEST_GST_LIB}")

#function(find_gst_libraries GST_LIB_LIST OUTPUT_VAR)
#    set(FOUND_LIBS)
#    foreach(LIB ${GST_LIB_LIST})
#        find_library(CURRENT_LIB
#                NAMES ${LIB}
#                NAMES_PER_DIR
#                PATHS ${GSTREAMER_ROOT}/lib
#                      ${GSTREAMER_ROOT}/lib/gstreamer-1.0
#                NO_DEFAULT_PATH
#        )
#        if(CURRENT_LIB)
#            list(APPEND FOUND_LIBS ${CURRENT_LIB})
#        else()
#            message(FATAL_ERROR "GStreamer library not found: ${LIB}")
#        endif()
#    endforeach()
#    set(${OUTPUT_VAR} ${FOUND_LIBS} PARENT_SCOPE)
#endfunction()
#
#
#
## Use it:
#find_gst_libraries("${GST_LIBS};${GST_PLUGINS_CORE_LIBS};${GST_PLUGINS_BASE}" GST_FULL_LIBS)

# ==========


target_link_libraries(
    native-lib
    ${log-lib} ${android-lib} ${media-lib} ${GST_FULL_LIBS})