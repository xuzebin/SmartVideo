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
double Classifier::setFeatures(int flow_count, int max_bin_index, double max_bin) {
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

double Classifier::calFeatures() {
//	acc_count.push(flow_count);






	return 0;
}






