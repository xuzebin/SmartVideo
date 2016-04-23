/*
 * Classifier.cpp
 *
 *  Created on: 2015年11月23日
 *      Author: xuzebin
 */

#include "Classifier.h"
using namespace std;

Classifier::Classifier() {
	// TODO Auto-generated constructor stub
	flow_count = 0;
	max_bin_index = 0;
	max_bin = 0;

//	flow_count_sum = 0;
	max_bin_sum = 0;
	max_bin_thres = 2000;

}

Classifier::~Classifier() {
	// TODO Auto-generated destructor stub
}

void Classifier::reset() {
	flow_count = 0;
	max_bin_index = 0;
	max_bin = 0;
}
double Classifier::recordFeatures(int flow_count, int max_bin_index, double max_bin) {
	this->flow_count = flow_count;
	this->max_bin_index = max_bin_index;
	this->max_bin = max_bin;

	const int ACCUM = 30;

	acc_max.push(max_bin);
	if (acc_max.size() <= ACCUM) {
		max_bin_sum += acc_max.back();
	} else { //size == 6
		double max_bin_avg = max_bin_sum / ACCUM;
//		if (max_bin_avg < 5) max_bin_thres = 40;
//		else if (max_bin_avg >= 5 && max_bin_avg < 10) max_bin_thres = 60;
//		else max_bin_thres = 80;

        //pop out the first element
        max_bin_sum -= acc_max.front();
        acc_max.pop();

        //add the last element to sum
        max_bin_sum += acc_max.back();
        return max_bin_avg;
	}
	return 0;
}

bool Classifier::classify(int flow_count, int max_bin_index, double max_bin, int time_stamp) {
	double max_bin_avg = recordFeatures(flow_count, max_bin_index, max_bin);

    bool isMotion = true;
	//exclude camera motion
	if (max_bin > 500 || flow_count > downsize[0] * downsize[1] / 6
					|| max_bin_avg > 400) {
		//camera motion
		isMotion = false;
	}

	//exclude static scene
	if (max_bin < 80 || flow_count < 80) {
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

int Classifier::getFirstIndex() {
	return first_index;
}
int Classifier::getLastIndex() {
	return last_index;
}





