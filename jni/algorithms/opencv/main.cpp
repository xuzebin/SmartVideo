#include <jni.h>
#include <android/log.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <iostream>

#define LOG_TAG "TestStitching"
#undef LOG
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,__VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, LOG_TAG,__VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN, LOG_TAG,__VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,__VA_ARGS__)
#define LOGF(...)  __android_log_print(ANDROID_LOG_FATAL, LOG_TAG,__VA_ARGS__)

void test_opencv()
{
	// cv::Mat is a image class
	// create opencv image object
	cv::Mat im(1280, 1920, CV_8UC3);
	LOGI("im.rows = %d, im.cols = %d, im.channels = %d\n", im.rows, im.cols, im.channels());

	cv::Mat im_small(240, 320, CV_8UC3);
	LOGI("im_small.rows = %d, im_small.cols = %d, im_small.channels = %d\n", im_small.rows, im_small.cols, im_small.channels());

	double t = (double)cv::getTickCount();//
	cv::resize(im, im_small, cv::Size(320, 240));
	t = ((double)cv::getTickCount() - t) / cv::getTickFrequency();
	LOGI("resize Time: %lfms\n", t * 1000);

	cv::Mat im_gray;
	cv::cvtColor(im, im_gray, CV_RGB2GRAY);
	LOGI("im_gray.rows = %d, im_gray.cols = %d, im_gray.channels = %d\n", im_gray.rows, im_gray.cols, im_gray.channels());

	cv::Mat im_gray_hist;
	cv::equalizeHist(im_gray, im_gray_hist);
	LOGI("im_gray_hist.rows = %d, im_gray_hist.cols = %d, im_gray_hist.channels = %d\n", im_gray_hist.rows, im_gray_hist.cols, im_gray_hist.channels());
}

int main()
{
	test_opencv();
	return 0;
}