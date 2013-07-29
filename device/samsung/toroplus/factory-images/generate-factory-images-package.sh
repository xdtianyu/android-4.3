#!/bin/sh

# Copyright 2012 The Android Open Source Project
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

source ../../../common/clear-factory-images-variables.sh
PRODUCT=toroplus
DEVICE=toroplus
BUILD=fh05
VERSION=fh05
SRCPREFIX=signed-
BOOTLOADER=primelc03
BOOTLOADERFILE=bootloader-toroplus.img
RADIO=l700.fc12
RADIOFILE=radio-toroplus.img
CDMARADIO=l700.fc12
CDMARADIOFILE=radio-cdma-toroplus.img
source ../../../common/generate-factory-images-common.sh
