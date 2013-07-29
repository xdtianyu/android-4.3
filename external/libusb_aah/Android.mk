# Copyright 2013 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# This makefile builds both for host and target, and so all the
# common definitions are factored out into a separate file to
# minimize duplication between the build rules.

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)

LOCAL_SRC_FILES := \
    libusb/core.c \
    libusb/descriptor.c \
    libusb/io.c \
    libusb/sync.c \
    libusb/os/linux_usbfs.c \
    libusb/os/threads_posix.c

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/libusb \
    $(LOCAL_PATH)/..

LOCAL_SHARED_LIBRARIES := \
    libc

LOCAL_MODULE:= libusb

include $(BUILD_SHARED_LIBRARY)
