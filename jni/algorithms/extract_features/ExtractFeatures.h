/*
 * ExtractFeatures.h
 *
 *  Created on: 2015年11月20日
 *      Author: xuzebin
 */

#ifndef EXTRACTFEATURES_H_
#define EXTRACTFEATURES_H_

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
using namespace cv;

class ExtractFeatures {
public:
	ExtractFeatures(int width, int height, int bin_num = 12);
	virtual ~ExtractFeatures();

    double count(float x, float y);
    double getMaxBin(int &max_index);
    double retrieve(int index);
    void clear();

    double calMagnitude(float x, float y);
    double calAngle(float x, float y);

//    //accumulate translations
//    void accumulateT(int x, int y);

    void normalize();//call after the histogram of each frame has been calculated
    double getMaxBinScale(int& max_index);

    double getMaxVelocity(int skip_frame_thres, int framerate);//get v from each bin and return the max velocity

    //split into 4 positive-only channels (ref: recognizing action at a distance)
    void splitChannels(Mat pos_x, Mat neg_x, Mat pos_y, Mat neg_y, int px, int py, float flowx, float flowy);

    int getFlowCount();
    int getSmallFlowCount();
    int getFlowEnergy();


private:
    double* bin_mag; //sum of magnitude in each bin
    int* bin_number;//sum of flow count in each bin
    int bin_count;

    int width, height;


    double* norm_bin;//normalized magnitude in each bin (all bins sum up to 1)
    double sum_bin;//sum of magnitude in all bins

    double* velocity;//average velocity in each bin

    int flow_count;
    int small_flow_count;

    /* the sum of magnitude of all optical flow vectors,
     * indicating moving extent
     */
    int flow_energy;

};

#endif /* EXTRACTFEATURES_H_ */
