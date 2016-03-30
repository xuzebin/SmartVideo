package com.example.smartVideoPlayer;

import java.lang.ref.WeakReference;

import com.example.utilities.Utils;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Used in Main thread, and handle messages sent from other threads 
 * @author xuzebin
 *
 */
public class MainHandler extends Handler {
	private static final String TAG = "MainHandler";
	
	private static final int MSG_FPS = 0;
	private static final int MSG_FLOW_COUNT = 1;	
	private static final int MSG_END_OF_STREAM = 2;
	private static final int MSG_SEEKBAR_PROGRESS = 3;
	private static final int MSG_SEEKBAR_MAX = 4;
	private static final int MSG_TIME_STAMP = 5;
	
	private WeakReference<VideoPlayerActivity> mWeakActivity;

    public MainHandler(VideoPlayerActivity activity) {
        mWeakActivity = new WeakReference<VideoPlayerActivity>(activity);
    }
    
    /* called in SpeedController */
    public void setFps(int fps) {
    	sendMessage(obtainMessage(MSG_FPS, fps, 0));
    }
    
    /* called in decoder thread */
    public void setTimeStamp(int pts) {
    	sendMessage(obtainMessage(MSG_TIME_STAMP, pts, 0));
    }
    public void getFlowCount(int pts) {
//    	sendMessage(obtainMessage(MSG_FLOW_COUNT, pts, 0));
    }
    public void signalEndOfStream() {
    	sendEmptyMessage(MSG_END_OF_STREAM);
    }  
    public void setSeekBarProgress(int progress) {
    	sendMessage(obtainMessage(MSG_SEEKBAR_PROGRESS, progress, 0));
    }
    public void setSeekBarMax(int max) {
    	sendMessage(obtainMessage(MSG_SEEKBAR_MAX, max, 0));
    }
    

	@Override
    public void handleMessage(Message msg) {

    	VideoPlayerActivity activity = mWeakActivity.get();
        if (activity == null) {
            Log.d(TAG, "Got message for dead activity");
            return;
        }
        switch (msg.what) {
            case MSG_FPS: {
            	activity.fps_view.setText(Integer.toString(msg.arg1) + " fps");
                break;
            }
            case MSG_FLOW_COUNT: {
            	activity.setFlowCountView(msg.arg1);
            	break;
            }
            case MSG_END_OF_STREAM: {
            	/* When video decoder reaches the end of stream, 
            	 * turn the button to "Play" state.
            	 */
            	activity.play_stop_btn.setText("Play");
            	break;
            }
            case MSG_SEEKBAR_PROGRESS: {
            	/* make sure the seekBar is not touched by users when setting a new progress */
            	if (!activity.seekBarListener.isTouch()) {
            		activity.seekBar.setProgress(msg.arg1);
            	}	
            	break;
            }
            case MSG_SEEKBAR_MAX: {
            	/* set the overall video time here */
            	activity.seekBar.setMax(msg.arg1);
        		String formattedTime = Utils.formatTime(msg.arg1);
        		activity.end_time_view.setText(formattedTime);
            	break;
            }
            case MSG_TIME_STAMP: {
//            	activity.time_stamp_view.setText(Integer.toString(msg.arg1) + " ms");
            	String formattedTime = Utils.formatTime(msg.arg1);
            	activity.start_time_view.setText(formattedTime);
            	/**
            	 * In autoMode, switch of playback speed is determined by indexes.
            	 * TODO: 1. make speed switching smoother
            	 */     	
                if (activity.isAutoMode()) {
                	if (activity.controller != null) {
                		activity.controller.setSmartPlaybackRate(activity.getFirstIndex(),
								activity.getLastIndex(), msg.arg1);
                	}
                	
                }
            	break;
            }
            	
            default:
                throw new RuntimeException("Unknown message " + msg.what);
        }
    }
}