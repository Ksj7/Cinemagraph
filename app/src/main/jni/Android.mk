LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := NeuQuantCompile
LOCAL_SRC_FILES := NeuQuant.cpp
LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)

