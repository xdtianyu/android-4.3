LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

ifeq "REL" "$(PLATFORM_VERSION_CODENAME)"
  RS_VERSION := $(PLATFORM_SDK_VERSION)
else
  # Increment by 1 whenever this is not a final release build, since we want to
  # be able to see the RS version number change during development.
  # See build/core/version_defaults.mk for more information about this.
  RS_VERSION := "(1 + $(PLATFORM_SDK_VERSION))"
endif
local_cflags_for_rs_cpp += -DRS_VERSION=$(RS_VERSION)

LOCAL_CFLAGS += $(local_cflags_for_rs_cpp)

LOCAL_SRC_FILES:= \
	RenderScript.cpp \
	BaseObj.cpp \
	Element.cpp \
	Type.cpp \
	Allocation.cpp \
	Script.cpp \
	ScriptC.cpp \
	ScriptIntrinsics.cpp

LOCAL_SHARED_LIBRARIES := \
	libRS \
	libz \
	libcutils \
	libutils \
	liblog

LOCAL_MODULE:= libRScpp

LOCAL_MODULE_TAGS := optional

intermediates := $(call intermediates-dir-for,STATIC_LIBRARIES,libRS,TARGET,)
librs_generated_headers := \
    $(intermediates)/rsgApiStructs.h \
    $(intermediates)/rsgApiFuncDecl.h
LOCAL_GENERATED_SOURCES := $(librs_generated_headers)

LOCAL_C_INCLUDES += frameworks/rs
LOCAL_C_INCLUDES += $(intermediates)


include $(BUILD_SHARED_LIBRARY)
