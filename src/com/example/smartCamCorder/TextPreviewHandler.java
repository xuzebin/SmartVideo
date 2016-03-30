package com.example.smartCamCorder;

import java.lang.ref.WeakReference;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Handler to update TextPreview UI in main thread
 * when algorithm thread calculates the new data.
 * 
 * @author xuzebin
 *
 */
public class TextPreviewHandler extends Handler {
	private static final String TAG = "TextPreviewHandler";
	
	private static final int POINT_COUNT = 0;
	private static final int DOWNSIZE = 1;
	private static final int AVG_MAX_BIN = 2;
	private static final int MAX_BIN = 3;
	private static final int MAX_BIN_INDEX = 4;
	private static final int FLOW_ENERGY = 5;   	
	private static final int INVALIDATE_VIEW = 6;
	private static final int FRAME_NUMBER = 7;
	private static final int SIGNAL_END = 8;
	private static final int TIME_STAMP = 9;
	
	
	private WeakReference<VideoRecorderActivity> mWeakActivity;
	
	public TextPreviewHandler(VideoRecorderActivity activity) {
		mWeakActivity = new WeakReference<VideoRecorderActivity>(activity);
	}
	
	@Override  // runs on UI thread
    public void handleMessage(Message msg) {
		VideoRecorderActivity activity = mWeakActivity.get();
		if (activity == null) {
            Log.w(TAG, "PreviewHandler.handleMessage: weak ref is null");
            return;
        }
		
		switch(msg.what) {
		case POINT_COUNT:
			activity.mTextPreview.setPointCount(msg.arg1);
			Log.i("debug", msg.arg1 + "");
			break;
		case DOWNSIZE:
			activity.mTextPreview.setDownsize(msg.arg1, msg.arg2);
			Log.i("debug", msg.arg1 + "");
			break;
		case AVG_MAX_BIN:
			float avg_maxbin = msg.arg1;
			activity.mTextPreview.setAvgMaxBin(avg_maxbin);
			break;
		case MAX_BIN:
			float maxbin = msg.arg1;
			activity.mTextPreview.setMaxBin(maxbin);
			break;
		case MAX_BIN_INDEX:
			activity.mTextPreview.setMaxBinIndex(msg.arg1);
			break;
		case FRAME_NUMBER:
			activity.mTextPreview.setFrameNumber(msg.arg1);
			break;
		case FLOW_ENERGY:
			activity.mTextPreview.setFlowEnergy(msg.arg1);
			break;
		case INVALIDATE_VIEW:
			activity.mTextPreview.invalidate();
			break;
		case SIGNAL_END:
			activity.mTextPreview.reset();
			break;
		case TIME_STAMP:
			activity.mTextPreview.setTimeStamp(msg.arg1);
			break;
		default:
            throw new RuntimeException("unknown msg " + msg.what);
		}	
	}
	
	public void updatePointCount(int pointCount) {
		sendMessage(obtainMessage(POINT_COUNT, pointCount, 0));
		Log.i("debug", "set pointcount: " + pointCount);
	}
	public void updateDownSize(int width, int height) {
		sendMessage(obtainMessage(DOWNSIZE, width, height));
	}
	
	public void updateAvgMaxBin(float avgMaxBinValue) {
		sendMessage(obtainMessage(AVG_MAX_BIN, (int)avgMaxBinValue, 0));
	}
	
	public void updateMaxBin(float maxBinValue) {
		sendMessage(obtainMessage(MAX_BIN, (int)maxBinValue, 0));
	}
	
	public void updateMaxBinIndex(int index) {
		sendMessage(obtainMessage(MAX_BIN_INDEX, index, 0));
	}
	
	public void updateFrameNumber(int frame) {
		sendMessage(obtainMessage(FRAME_NUMBER, frame, 0));
	}
	
	public void updateFlowEnergy(int flowEnergy) {
		sendMessage(obtainMessage(FLOW_ENERGY, flowEnergy, 0));
	}
	
	public void invalidateView() {
		sendEmptyMessage(INVALIDATE_VIEW);
	}
	
	public void signalEnd() {
		sendEmptyMessage(SIGNAL_END);
	}
	
	public void updateTimeStamp(int timeStamp) {
		sendMessage(obtainMessage(TIME_STAMP, timeStamp, 0));
	}
	
}