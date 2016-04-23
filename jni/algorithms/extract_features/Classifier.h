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
	double recordFeatures(int flow_count, int max_bin_index, double max_bin);

	double classify(int flow_count, int max_bin_index, double max_bin, int time_stamp);
	int getFirstIndex();
	int getLastIndex();

private:
	int flow_count;
	int max_bin_index;
	double max_bin;

//    queue<int> acc_count;
    std::queue<double> acc_max;

//    double flow_count_sum;

    double max_bin_sum;
    double max_bin_thres;


    //classification result
    int first_index;
    int last_index;



};

#endif /* CLASSIFIER_H_ */
