
LOCAL_PATH:=$(call my-dir)

rs_base_CFLAGS := -Werror -Wall -Wno-unused-parameter -Wno-unused-variable
ifeq ($(TARGET_BUILD_PDK), true)
  rs_base_CFLAGS += -D__RS_PDK__
endif

ifneq ($(OVERRIDE_RS_DRIVER),)
  rs_base_CFLAGS += -DOVERRIDE_RS_DRIVER=$(OVERRIDE_RS_DRIVER)
endif

include $(CLEAR_VARS)
LOCAL_CLANG := true
LOCAL_MODULE := libRSDriver

LOCAL_SRC_FILES:= \
	driver/rsdAllocation.cpp \
	driver/rsdBcc.cpp \
	driver/rsdCore.cpp \
	driver/rsdFrameBuffer.cpp \
	driver/rsdFrameBufferObj.cpp \
	driver/rsdGL.cpp \
	driver/rsdMesh.cpp \
	driver/rsdMeshObj.cpp \
	driver/rsdPath.cpp \
	driver/rsdProgram.cpp \
	driver/rsdProgramRaster.cpp \
	driver/rsdProgramStore.cpp \
	driver/rsdRuntimeStubs.cpp \
	driver/rsdSampler.cpp \
	driver/rsdScriptGroup.cpp \
	driver/rsdShader.cpp \
	driver/rsdShaderCache.cpp \
	driver/rsdVertexArray.cpp


LOCAL_SHARED_LIBRARIES += libRS libRSCpuRef
LOCAL_SHARED_LIBRARIES += liblog libcutils libutils libEGL libGLESv1_CM libGLESv2
LOCAL_SHARED_LIBRARIES += libbcc libbcinfo libLLVM libui libgui libsync

LOCAL_C_INCLUDES += frameworks/compile/libbcc/include

LOCAL_CFLAGS += $(rs_base_CFLAGS)

LOCAL_LDLIBS := -lpthread -ldl
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

# Build rsg-generator ====================
include $(CLEAR_VARS)

LOCAL_MODULE := rsg-generator

# These symbols are normally defined by BUILD_XXX, but we need to define them
# here so that local-intermediates-dir works.

LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
intermediates := $(local-intermediates-dir)

LOCAL_SRC_FILES:= \
    spec.l \
    rsg_generator.c

include $(BUILD_HOST_EXECUTABLE)

# TODO: This should go into build/core/config.mk
RSG_GENERATOR:=$(LOCAL_BUILT_MODULE)

include $(CLEAR_VARS)
LOCAL_CLANG := true
LOCAL_MODULE := libRS

LOCAL_MODULE_CLASS := SHARED_LIBRARIES
intermediates:= $(local-intermediates-dir)

# Generate custom headers

GEN := $(addprefix $(intermediates)/, \
            rsgApiStructs.h \
            rsgApiFuncDecl.h \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = cat $(PRIVATE_PATH)/rs.spec $(PRIVATE_PATH)/rsg.spec $(PRIVATE_PATH)/rs_native.spec | $(RSG_GENERATOR) $< $@
$(GEN) : $(RSG_GENERATOR) $(LOCAL_PATH)/rs.spec $(LOCAL_PATH)/rsg.spec $(LOCAL_PATH)/rs_native.spec
$(GEN): $(intermediates)/%.h : $(LOCAL_PATH)/%.h.rsg
	$(transform-generated-source)

# used in jni/Android.mk
rs_generated_source += $(GEN)
LOCAL_GENERATED_SOURCES += $(GEN)

# Generate custom source files

GEN := $(addprefix $(intermediates)/, \
            rsgApi.cpp \
            rsgApiReplay.cpp \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = cat $(PRIVATE_PATH)/rs.spec $(PRIVATE_PATH)/rsg.spec $(PRIVATE_PATH)/rs_native.spec | $(RSG_GENERATOR) $< $@
$(GEN) : $(RSG_GENERATOR) $(LOCAL_PATH)/rs.spec $(LOCAL_PATH)/rsg.spec $(LOCAL_PATH)/rs_native.spec
$(GEN): $(intermediates)/%.cpp : $(LOCAL_PATH)/%.cpp.rsg
	$(transform-generated-source)

# used in jni/Android.mk
rs_generated_source += $(GEN)

LOCAL_GENERATED_SOURCES += $(GEN)

LOCAL_SRC_FILES:= \
	rsAdapter.cpp \
	rsAllocation.cpp \
	rsAnimation.cpp \
	rsComponent.cpp \
	rsContext.cpp \
	rsDevice.cpp \
	rsElement.cpp \
	rsFBOCache.cpp \
	rsFifoSocket.cpp \
	rsFileA3D.cpp \
	rsFont.cpp \
	rsObjectBase.cpp \
	rsMatrix2x2.cpp \
	rsMatrix3x3.cpp \
	rsMatrix4x4.cpp \
	rsMesh.cpp \
	rsMutex.cpp \
	rsPath.cpp \
	rsProgram.cpp \
	rsProgramFragment.cpp \
	rsProgramStore.cpp \
	rsProgramRaster.cpp \
	rsProgramVertex.cpp \
	rsSampler.cpp \
	rsScript.cpp \
	rsScriptC.cpp \
	rsScriptC_Lib.cpp \
	rsScriptC_LibGL.cpp \
	rsScriptGroup.cpp \
	rsScriptIntrinsic.cpp \
	rsSignal.cpp \
	rsStream.cpp \
	rsThreadIO.cpp \
	rsType.cpp

LOCAL_SHARED_LIBRARIES += liblog libcutils libutils libEGL libGLESv1_CM libGLESv2 libbcc
LOCAL_SHARED_LIBRARIES += libui libbcinfo libLLVM libgui libsync libdl

LOCAL_STATIC_LIBRARIES := libft2

LOCAL_C_INCLUDES += external/freetype/include
LOCAL_C_INCLUDES += frameworks/compile/libbcc/include

LOCAL_CFLAGS += $(rs_base_CFLAGS)

LOCAL_LDLIBS := -lpthread -ldl
LOCAL_MODULE_TAGS := optional

include $(BUILD_SHARED_LIBRARY)

# Now build a host version for serialization
include $(CLEAR_VARS)
LOCAL_MODULE:= libRS
LOCAL_MODULE_TAGS := optional

intermediates := $(call intermediates-dir-for,STATIC_LIBRARIES,libRS,HOST,)

# Generate custom headers

GEN := $(addprefix $(intermediates)/, \
            rsgApiStructs.h \
            rsgApiFuncDecl.h \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = cat $(PRIVATE_PATH)/rs.spec $(PRIVATE_PATH)/rsg.spec $(PRIVATE_PATH)/rs_native.spec | $(RSG_GENERATOR) $< $@
$(GEN) : $(RSG_GENERATOR) $(LOCAL_PATH)/rs.spec $(LOCAL_PATH)/rsg.spec $(LOCAL_PATH)/rs_native.spec
$(GEN): $(intermediates)/%.h : $(LOCAL_PATH)/%.h.rsg
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)

# Generate custom source files

GEN := $(addprefix $(intermediates)/, \
            rsgApi.cpp \
            rsgApiReplay.cpp \
        )

$(GEN) : PRIVATE_PATH := $(LOCAL_PATH)
$(GEN) : PRIVATE_CUSTOM_TOOL = cat $(PRIVATE_PATH)/rs.spec $(PRIVATE_PATH)/rsg.spec $(PRIVATE_PATH)/rs_native.spec | $(RSG_GENERATOR) $< $@
$(GEN) : $(RSG_GENERATOR) $(LOCAL_PATH)/rs.spec $(LOCAL_PATH)/rsg.spec $(LOCAL_PATH)/rs_native.spec
$(GEN): $(intermediates)/%.cpp : $(LOCAL_PATH)/%.cpp.rsg
	$(transform-generated-source)

LOCAL_GENERATED_SOURCES += $(GEN)

LOCAL_CFLAGS += $(rs_base_CFLAGS)
LOCAL_CFLAGS += -DANDROID_RS_SERIALIZE
LOCAL_CFLAGS += -fPIC

LOCAL_SRC_FILES:= \
	rsAdapter.cpp \
	rsAllocation.cpp \
	rsAnimation.cpp \
	rsComponent.cpp \
	rsContext.cpp \
	rsDevice.cpp \
	rsElement.cpp \
	rsFBOCache.cpp \
	rsFifoSocket.cpp \
	rsFileA3D.cpp \
	rsFont.cpp \
	rsObjectBase.cpp \
	rsMatrix2x2.cpp \
	rsMatrix3x3.cpp \
	rsMatrix4x4.cpp \
	rsMesh.cpp \
	rsMutex.cpp \
	rsPath.cpp \
	rsProgram.cpp \
	rsProgramFragment.cpp \
	rsProgramStore.cpp \
	rsProgramRaster.cpp \
	rsProgramVertex.cpp \
	rsSampler.cpp \
	rsScript.cpp \
	rsScriptC.cpp \
	rsScriptC_Lib.cpp \
	rsScriptC_LibGL.cpp \
	rsScriptGroup.cpp \
	rsScriptIntrinsic.cpp \
	rsSignal.cpp \
	rsStream.cpp \
	rsThreadIO.cpp \
	rsType.cpp

LOCAL_STATIC_LIBRARIES := libcutils libutils liblog

LOCAL_LDLIBS := -lpthread

include $(BUILD_HOST_STATIC_LIBRARY)


LLVM_ROOT_PATH := external/llvm

#=============================================================================
# android librsloader for libbcc (Device)
#-----------------------------------------------------------------------------

rsloader_SRC_FILES := \
  driver/linkloader/android/librsloader.cpp \
  driver/linkloader/lib/ELFHeader.cpp \
  driver/linkloader/lib/ELFSymbol.cpp \
  driver/linkloader/lib/ELFSectionHeader.cpp \
  driver/linkloader/lib/ELFTypes.cpp \
  driver/linkloader/lib/GOT.cpp \
  driver/linkloader/lib/MemChunk.cpp \
  driver/linkloader/lib/StubLayout.cpp \
  driver/linkloader/utils/helper.cpp \
  driver/linkloader/utils/raw_ostream.cpp \
  driver/linkloader/utils/rsl_assert.cpp

include $(CLEAR_VARS)

LOCAL_MODULE := librsloader

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(rsloader_SRC_FILES)

LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_CFLAGS += $(rs_base_CFLAGS)

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/driver/linkloader \
  $(LOCAL_PATH)/driver/linkloader/include \
  $(LOCAL_C_INCLUDES)

include $(LLVM_ROOT_PATH)/llvm-device-build.mk
include $(BUILD_STATIC_LIBRARY)


#=============================================================================
# android librsloader for libbcc (Host)
#-----------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE := librsloader

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(rsloader_SRC_FILES)

LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_CFLAGS += $(rs_base_CFLAGS)
LOCAL_CFLAGS += -D__HOST__

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/driver/linkloader \
  $(LOCAL_PATH)/driver/linkloader/include \
  $(LOCAL_C_INCLUDES)

include $(LLVM_ROOT_PATH)/llvm-host-build.mk
include $(BUILD_HOST_STATIC_LIBRARY)


#=============================================================================
# librsloader-test (Device)
#-----------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE := test-librsloader

LOCAL_MODULE_TAGS := tests

LOCAL_SHARED_LIBRARIES := \
  libstlport

LOCAL_STATIC_LIBRARIES := \
  librsloader \
  libcutils \
  liblog \
  libLLVMSupport

LOCAL_SRC_FILES := \
  driver/linkloader/android/test-librsloader.c

LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_CFLAGS += $(rs_base_CFLAGS)

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/driver/linkloader \
  $(LOCAL_PATH)/driver/linkloader/include

include $(LLVM_ROOT_PATH)/llvm-device-build.mk
include $(BUILD_EXECUTABLE)


#=============================================================================
# librsloader-test (Host)
#-----------------------------------------------------------------------------

include $(CLEAR_VARS)

LOCAL_MODULE := test-librsloader

LOCAL_MODULE_TAGS := tests

LOCAL_LDLIBS := \
  -lpthread \
  -ldl

LOCAL_STATIC_LIBRARIES := \
  librsloader \
  libcutils \
  liblog \
  libLLVMSupport

LOCAL_SRC_FILES := \
  driver/linkloader/android/test-librsloader.c

LOCAL_ADDITIONAL_DEPENDENCIES := $(LOCAL_PATH)/Android.mk

LOCAL_CFLAGS += $(rs_base_CFLAGS)

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/driver/linkloader \
  $(LOCAL_PATH)/driver/linkloader/include

include $(LLVM_ROOT_PATH)/llvm-host-build.mk
include $(BUILD_HOST_EXECUTABLE)


#=============================================================================
# rsloader
#-----------------------------------------------------------------------------

ifdef BUILD_RSLOADER_TOOL
include $(CLEAR_VARS)

LOCAL_MODULE := rsloader

LOCAL_MODULE_TAGS := tests

LOCAL_SHARED_LIBRARIES := \
  libstlport

LOCAL_STATIC_LIBRARIES := \
  libLLVMSupport

LOCAL_SRC_FILES := \
  driver/linkloader/lib/ELFHeader.cpp \
  driver/linkloader/lib/ELFSymbol.cpp \
  driver/linkloader/lib/ELFSectionHeader.cpp \
  driver/linkloader/lib/ELFTypes.cpp \
  driver/linkloader/lib/StubLayout.cpp \
  driver/linkloader/utils/raw_ostream.cpp \
  driver/linkloader/utils/rsl_assert.cpp \
  driver/linkloader/utils/helper.cpp \
  driver/linkloader/main.cpp

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/driver/linkloader \
  $(LOCAL_PATH)/driver/linkloader/include \
  $(LOCAL_C_INCLUDES)

include $(LLVM_ROOT_PATH)/llvm-device-build.mk
include $(BUILD_EXECUTABLE)
endif


#=============================================================================
# stub-layout-unit-test
#-----------------------------------------------------------------------------

ifdef BUILD_STUB_LAYOUT_TEST
include $(CLEAR_VARS)

LOCAL_MODULE := stub-layout-unit-test

LOCAL_MODULE_TAGS := tests

LOCAL_SHARED_LIBRARIES := \
  libstlport

LOCAL_SRC_FILES := \
  driver/linkloader/lib/StubLayout.cpp \
  driver/linkloader/utils/raw_ostream.cpp \
  driver/linkloader/utils/helper.cpp \
  driver/linkloader/tests/stub-test.cpp

LOCAL_C_INCLUDES := \
  $(LOCAL_PATH)/driver/linkloader \
  $(LOCAL_PATH)/driver/linkloader/include \
  $(LOCAL_C_INCLUDES)

include $(LLVM_ROOT_PATH)/llvm-device-build.mk
include $(BUILD_EXECUTABLE)
endif


include $(call all-makefiles-under,$(LOCAL_PATH))
