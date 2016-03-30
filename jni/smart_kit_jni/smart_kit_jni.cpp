#define _ANDROID_
#define DEBUG

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/video/tracking.hpp>
//#include <iostream>
#include <jni.h>
#include "log.h"
#include "../algorithms/extract_features/ExtractFeatures.h"
#include "../algorithms/extract_features/Classifier.h"

#include <cstring>
#include <vector>
#include <float.h>


/* GLOBAL VARIABLES */
int downsize[2] = {64, 36};//36; //size of downsize
float debug_values[32]; //debug values for display

int first_index = 0;
int last_index = 0;

const long MSEC_PER_FRAME = 30;//unit: millisecond
const long SKIP_FRAMES_NUMBER = 30;


int dense_optical_flow(unsigned char* yuvframe, int width, int height, int frame_index, long time_stamp);

extern "C" JNIEXPORT jfloatArray Java_com_example_smartNative_SmartNative_getDebugValue( JNIEnv* env,
        jobject thiz)
 {
    SMART_DEBUG("%s E", __func__);
    jfloatArray jniDebugvalue = env->NewFloatArray(32);
    env->SetFloatArrayRegion(jniDebugvalue, 0, 32, debug_values);
    SMART_DEBUG("%s X", __func__);
    return jniDebugvalue;
}


//extern "C" JNIEXPORT jint Java_com_example_smartNative_SmartNative_getPointCountFromOpticalFlow( JNIEnv* env,
//        jobject thiz,
//        jbyteArray data,
//        int width,
//        int height)
//{
//    jbyte* yuvframe = env->GetByteArrayElements(data, NULL);
//
//    int point_count = dense_optical_flow((unsigned char *) yuvframe, width, height, debugValues);
//
//    env->ReleaseByteArrayElements(data, yuvframe, 0);
//    return point_count;
//}
bool isMotion = true;
bool judgeIndexes(int current_index)
{
	isMotion = true;
	//exclude camera motion
	if (debug_values[10] > 500 || debug_values[11] > downsize[0] * downsize[1] / 6
					|| debug_values[12] > 400) {
		//camera motion
		isMotion = false;
	}

	//exclude static scene
	if (debug_values[10] < 80 || debug_values[11] < 80) {
		//static
		isMotion = false;
	}

	if (isMotion) {//object motion
		if (current_index > 30) {//avoid the initial instability
			if (first_index == 0) {
				first_index = current_index;//record the first motion
			}

			last_index = current_index;//record the last motion
		}

	}
	return isMotion;
}

bool judgeTimeStamps(int time_stamp)//unit: millisecond
{
	isMotion = true;
	//exclude camera motion
	if (debug_values[10] > 500 || debug_values[11] > downsize[0] * downsize[1] / 6
					|| debug_values[12] > 400) {
		//camera motion
		isMotion = false;
	}

	//exclude static scene
	if (debug_values[10] < 80 || debug_values[11] < 80) {
		//static
		isMotion = false;
	}

	if (isMotion) {//object motion
		if (time_stamp > SKIP_FRAMES_NUMBER * MSEC_PER_FRAME) {//avoid the initial instability
			if (first_index == 0) {
				first_index = time_stamp;//record the first motion
			}

			last_index = time_stamp;//record the last motion
		}

	}
	return isMotion;
}

extern "C" JNIEXPORT jint Java_com_example_smartNative_SmartNative_calOpticalFlow( JNIEnv* env,
        jobject thiz,
        jbyteArray data,
        int width,
        int height, int frame_index, long time_stamp)
{
    jbyte* yuvframe = env->GetByteArrayElements(data, NULL);

    int point_count = dense_optical_flow((unsigned char *) yuvframe, width, height, frame_index, time_stamp);

    env->ReleaseByteArrayElements(data, yuvframe, 0);
    return point_count;
}



extern "C" JNIEXPORT jintArray Java_com_example_smartNative_SmartNative_getResultIndexes( JNIEnv* env,
        jobject thiz)
{
	int result_indxes[2] = {first_index, last_index};
	jintArray jniIndexes = env->NewIntArray(2);
	env->SetIntArrayRegion(jniIndexes, 0, 2, result_indxes);

	first_index = 0;
	last_index = 0;
	return jniIndexes;
}
extern "C" JNIEXPORT jintArray Java_com_example_smartNative_SmartNative_getDownSize( JNIEnv* env,
        jobject thiz)
{
	jintArray jniDownSize = env->NewIntArray(2);
	env->SetIntArrayRegion(jniDownSize, 0, 2, downsize);
	return jniDownSize;
}

//
//  DenseOpticalFlow
//
//  Created by xuzebin on 15/9/2.
//  Copyright (c) 2015 xuzebin. All rights reserved.
//
// Gunnar Farneback 2D dense optical flow algorithm
// call API calcOpticalFlowFarneback() in OpenCV

using namespace cv;
cv::Mat prevgray, gray, flow;//global variables




ExtractFeatures* extract_features = new ExtractFeatures(downsize[0], downsize[1], 12);//create a histogram with 12 bins
Classifier* classifier = new Classifier();
int dense_optical_flow(unsigned char* yuvframe, int width, int height, int frame_index, long time_stamp)
{
    /* a trick to calculate after 50 frames to avoid initial unstable values */
//    static int flag = 0;
//    if (flag++ < 50) return 0;

    /* a trick to skip frames */
//  static int skip_flag = 0;
//  skip_flag = skip_flag < 1 ? skip_flag + 1 : 0;
//  if (skip_flag) return last_point_count;

    //cost time calculations -- start
    double t = (double)cvGetTickCount();
//    long t2 = clock();

    //convert yuv format to 1 channel Mat
    cv::Mat frame = Mat(height, width, CV_8UC1, yuvframe);

    //downsize frame to gray
    cv::resize(frame, gray, Size(downsize[0], downsize[1]));

//    cv::GaussianBlur(gray, gray, Size(7, 7), 0, 0);

    //number of moving optical flow points
    int point_count = 0;

    double cc = 0;
    extract_features->clear();
    classifier->reset();
    if( prevgray.data )
    {
//      cvCalcOpticalFlowFarneback((CvArr*)&prevgray, (CvArr*)&gray, (CvArr*)&flow, 0.5, 3, 15, 3, 5, 1.2, 0);

        //Gunnar Farneback 2D dense optical flow algorithm
        cv::calcOpticalFlowFarneback(prevgray, gray, flow, 0.5, 3, 15, 3, 5, 1.2, 0);
//    	cv::calcOpticalFlowFarneback(prevgray, gray, flow, 0.1, 1, 15, 3, 5, 1.2, 0);

        //count the number of moving optical flow points
        for (int y = 0; y < flow.rows; y += 2) {
                for (int x = 0; x < flow.cols; x += 2) {
                    Point2f fxy = flow.at<Point2f>(y, x);
//                    if (x - cvRound(x+fxy.x) == 0 && y - cvRound(y+fxy.y) == 0) continue;//ignore invariant point
//                    if (x - cvRound(x+fxy.x) <= 0.3 && y - cvRound(y+fxy.y) <= 0.3) continue;//ignore invariant point
                    cc = extract_features->count(fxy.x, fxy.y);
                    if (cc == 0) continue;//if the moving is too little

                    point_count++;//count the number of moving Optical Flow points

                }
         }
        debug_values[7] = extract_features->getSmallFlowCount();

        debug_values[8] = extract_features->getFlowEnergy();

        int max_bin_index = 0;
        double max_bin = extract_features->getMaxBin(max_bin_index);
        debug_values[9] = max_bin_index;
        debug_values[10] = max_bin;
        debug_values[11] = extract_features->getFlowCount();

        float avg = classifier->setFeatures(extract_features->getFlowCount(), max_bin_index, max_bin);
        debug_values[12] = avg;

//        judgeIndexes(frame_index);
        judgeTimeStamps(time_stamp);

    }


    //copy the current frame to the previous frame before next round
    std::swap(prevgray, gray);


    return point_count;
}
