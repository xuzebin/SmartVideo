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

SMART_DIR = $(LOCAL_PATH)

LOCAL_C_INCLUDES += $(SMART_DIR)/opencv/include
LOCAL_C_INCLUDES += $(SMART_DIR)/extract_features

SRC_FILES := $(wildcard $(SMART_DIR)/extract_features/*.cpp)

LOCAL_SRC_FILES := opencv/src/core/gpumat.cpp opencv/src/core/algorithm.cpp opencv/src/core/opengl_interop_deprecated.cpp \
opencv/src/core/system.cpp opencv/src/core/alloc.cpp opencv/src/core/datastructs.cpp opencv/src/core/lapack.cpp \
opencv/src/core/out.cpp opencv/src/core/tables.cpp opencv/src/core/arithm.cpp opencv/src/core/drawing.cpp \
opencv/src/core/mathfuncs.cpp opencv/src/core/parallel.cpp opencv/src/core/array.cpp opencv/src/core/dxt.cpp \
opencv/src/core/matmul.cpp opencv/src/core/persistence.cpp opencv/src/core/cmdparser.cpp opencv/src/core/gl_core_3_1.cpp \
opencv/src/core/matop.cpp opencv/src/core/convert.cpp opencv/src/core/matrix.cpp opencv/src/core/rand.cpp \
opencv/src/core/copy.cpp opencv/src/core/glob.cpp opencv/src/core/opengl_interop.cpp opencv/src/core/stat.cpp \
opencv/src/imgproc/color.cpp opencv/src/imgproc/featureselect.cpp opencv/src/imgproc/histogram.cpp \
opencv/src/imgproc/sumpixels.cpp opencv/src/imgproc/contours.cpp opencv/src/imgproc/filter.cpp \
opencv/src/imgproc/hough.cpp opencv/src/imgproc/pyramids.cpp opencv/src/imgproc/tables.cpp \
opencv/src/imgproc/accum.cpp opencv/src/imgproc/convhull.cpp opencv/src/imgproc/floodfill.cpp \
opencv/src/imgproc/imgwarp.cpp opencv/src/imgproc/rotcalipers.cpp opencv/src/imgproc/templmatch.cpp \
opencv/src/imgproc/approx.cpp opencv/src/imgproc/corner.cpp opencv/src/imgproc/gabor.cpp \
opencv/src/imgproc/linefit.cpp opencv/src/imgproc/samplers.cpp opencv/src/imgproc/thresh.cpp \
opencv/src/imgproc/cornersubpix.cpp opencv/src/imgproc/matchcontours.cpp opencv/src/imgproc/segmentation.cpp \
opencv/src/imgproc/undistort.cpp opencv/src/imgproc/deriv.cpp opencv/src/imgproc/generalized_hough.cpp \
opencv/src/imgproc/moments.cpp opencv/src/imgproc/shapedescr.cpp opencv/src/imgproc/utils.cpp \
opencv/src/imgproc/canny.cpp opencv/src/imgproc/distransform.cpp  opencv/src/imgproc/geometry.cpp \
opencv/src/imgproc/morph.cpp opencv/src/imgproc/smooth.cpp opencv/src/imgproc/clahe.cpp \
opencv/src/imgproc/emd.cpp opencv/src/imgproc/grabcut.cpp opencv/src/imgproc/phasecorr.cpp opencv/src/imgproc/subdivision2d.cpp \
opencv/src/video/bgfg_gaussmix.cpp opencv/src/video/bgfg_gaussmix2.cpp opencv/src/video/bgfg_gmg.cpp opencv/src/video/camshift.cpp \
opencv/src/video/kalman.cpp opencv/src/video/lkpyramid.cpp opencv/src/video/motempl.cpp \
opencv/src/video/optflowgf.cpp opencv/src/video/simpleflow.cpp opencv/src/video/video_init.cpp  opencv/src/video/tvl1flow.cpp

LOCAL_SRC_FILES += $(SRC_FILES:$(SMART_DIR)/%=%)

LOCAL_LDLIBS +=  -lz -lm -llog -landroid

LOCAL_MODULE    := algorithms

include $(BUILD_SHARED_LIBRARY)
