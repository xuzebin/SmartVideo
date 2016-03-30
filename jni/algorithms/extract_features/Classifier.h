/*
 * Classifier.h
 *
 *  Created on: 2015年11月23日
 *      Author: xuzebin
 */

#ifndef CLASSIFIER_H_
#define CLASSIFIER_H_

#include <queue>

class Classifier {
public:
	Classifier();
	virtual ~Classifier();

	void reset();
	double setFeatures(int flow_count, int max_bin_index, double max_bin);

	double calFeatures();

private:
	int flow_count;
	int max_bin_index;
	double max_bin;

//    queue<int> acc_count;
    std::queue<double> acc_max;

//    double flow_count_sum;

    double max_bin_sum;
    double max_bin_thres;



};

#endif /* CLASSIFIER_H_ */
