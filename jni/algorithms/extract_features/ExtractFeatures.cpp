/*
 * ExtractFeatures.cpp
 *
 *  Created on: 2015年11月20日
 *      Author: xuzebin
 */

#include "ExtractFeatures.h"
#include <string.h>
#include <cmath>

#define CIRCLE 360

ExtractFeatures::ExtractFeatures(int width, int height, int bin_count) {
	// TODO Auto-generated constructor stub
    this->width = width;
    this->height = height;
    this->bin_count = bin_count;

    bin_mag = new double[bin_count];//sum of magnitude for each bin
    norm_bin = new double[bin_count];//normalized maginutude for each bin (sum of mags in all bins = 1)
    bin_number = new int[bin_count];//flow count in each bin(magnitude is not considered)
    velocity = new double[bin_count];//average velocity for each bin
    clear();


}

ExtractFeatures::~ExtractFeatures() {
	// TODO Auto-generated destructor stub
    delete[] bin_mag;
    delete[] norm_bin;
    delete[] bin_number;
    delete[] velocity;
}

void ExtractFeatures::clear()
{
    memset(bin_mag, 0, bin_count * sizeof(double));
    memset(norm_bin, 0, bin_count * sizeof(double));
    memset(bin_number, 0, bin_count * sizeof(int));
    memset(velocity, 0, bin_count * sizeof(double));
    sum_bin = 0;
    flow_count = 0;
    small_flow_count = 0;
    flow_energy = 0;
}

int abs(int x) {
	return x >= 0 ? x : -x;
}

double ExtractFeatures::count(float x, float y)
{
    if (abs(x) < 5 && abs(y) < 5 && abs(x) > 1 && abs(y) > 1) {
    	small_flow_count++;
    }
    if (abs(x) < 0.01 && abs(y) < 0.01) return 0;

	flow_count++;

    double magnitude = calMagnitude(x, y);
    double angle = calAngle(x, y);

    flow_energy += magnitude;//sum of all optical flow vectors' magnitude

    int interval = CIRCLE / bin_count;
    for (int i = 0; i < bin_count; ++i) {
        if (angle >= i * interval && angle < (i + 1) * interval) {
            bin_mag[i] += magnitude;
            bin_number[i]++;
            sum_bin += magnitude;
            return bin_mag[i];
        }
    }

    return 0;
}

double ExtractFeatures::getMaxBin(int &max_index)
{
    double max = 0;
    for (int i = 0; i < bin_count; ++i) {
        if (max < bin_mag[i]) {
            max = bin_mag[i];
            max_index = i;
        }
    }
    return max;
}

double ExtractFeatures::retrieve(int index)
{
    if (index >= bin_count || index < 0) return 0;
    return bin_mag[index];
}



double ExtractFeatures::calMagnitude(float x, float y)
{
    return sqrt(x * x + y * y);
}

double ExtractFeatures::calAngle(float x, float y)
{
    double angle = atan2((double)y, (double)x) * 180 / CV_PI;
    angle = angle < 0 ? angle + 360 : angle;//normalize to 0 - 360
    return angle;
}

//void OriHist::accumulateT(int x, int y)
//{
//    long code = (long)x * 1000 + y;
//    if (acc_trans.find(code) == acc_trans.end()) {//not exsit
//        acc_trans[code] = 1;
//    } else {
//        acc_trans[code] += 1;
//    }
//}

void ExtractFeatures::normalize()
{
    for (int i = 0; i < bin_count; ++i) {
        norm_bin[i] = (double)bin_mag[i] / sum_bin;
    }
}

double ExtractFeatures::getMaxBinScale(int& max_index)
{
    normalize();
    double max = 0;
    for (int i = 0; i < bin_count; ++i) {
        if (max < norm_bin[i]) {
            max = norm_bin[i];
            max_index = i;
        }
    }
    return max;
}

double ExtractFeatures::getMaxVelocity(int skip_frame_thres, int framerate)
{
    double avg_mag;
    double time = (double)2 * (skip_frame_thres + 1) / framerate;//2 frames time


    double max_v = 0;
    //get average magnitude for each bin
    for (int i = 0; i < bin_count; ++i) {
        avg_mag = (double)bin_mag[i] / bin_number[i];
        velocity[i] = avg_mag / time;
        if (max_v < velocity[i]) max_v = velocity[i];
    }
//    std::cout << "time = " << time << "max_v= " << max_v << std::endl;
    return max_v;
}

void ExtractFeatures::splitChannels(Mat pos_x, Mat neg_x, Mat pos_y, Mat neg_y, int px, int py, float flowx, float flowy)
{
    int colorx, colory;
    colorx = 255 - abs(flowx) * 40;
    if (colorx < 0) colorx = 0;
    colory = 255 - abs(flowy) * 40;
    if (colory < 0) colory = 0;

    if (flowx > 0) {
        line(pos_x, Point(px, py), Point(cvRound(px + flowx), py), colorx);
    } else if (flowx < 0) {
        line(neg_x, Point(px, py), Point(cvRound(px - flowx), py), colorx);
    }

    if (flowy > 0) {
        line(pos_y, Point(px, py), Point(px, cvRound(py + flowy)), colory);
    } else if (flowy < 0) {
        line(neg_y, Point(px, py), Point(px, cvRound(py - flowy)), colory);
    }

}

int ExtractFeatures::getFlowCount()
{
	return flow_count;
}

int ExtractFeatures::getSmallFlowCount()
{
	return small_flow_count;
}

int ExtractFeatures::getFlowEnergy()
{
	return flow_energy;
}
