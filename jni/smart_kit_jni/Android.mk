# Copyright (C) 2009 The Android Open Source Project
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
#
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../algorithms/opencv/include
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../algorithms/extract_features

LOCAL_SRC_FILES := smart_kit_jni.cpp

LOCAL_LDLIBS +=  -lz -lm -llog -landroid

LOCAL_SHARED_LIBRARIES += algorithms

LOCAL_MODULE    := smart_kit_jni

include $(BUILD_SHARED_LIBRARY)
