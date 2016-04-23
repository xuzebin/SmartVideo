package com.example.smartVideoPlayer;

import android.util.Log;

import com.example.smartVideoPlayer.VideoDecoder.SpeedController;
import com.example.smartVideoPlayer.MainHandler;

/**
 * This is the improved SpeedController based on Grafika's.
 * This is called in decoder thread and main thread.
 * @author xuzebin
 * 
 * Added 2 functions:
 * 1. Frame rate change at any time during playback.
 * 2. Pause and resume. Corrections of presentation time stamps offset due to pausing.
 * 
 * TODO: 
 * 1. add interfaces for manually frame rate change from 0 - 100 fps. 
 * (From my experiments, the hardware decoder using MediaCodec can decode frames up to 100fps,
 * 	but this was tested only on Lenovo X2, using outputSurface for decoder)
 *
 */
public class MySpeedController implements SpeedController {
	private static final String TAG = "MySpeedController";
    private static final long ONE_MILLION = 1000000L;
    private static final int NORMAL_FPS = 10;//30;
    private static final int MAX_FPS = 30;//240;
//    private static final int MIN_FPS = 10;
//    private static final int FPS_CHANGE_RATE = 10; //per 100 msec
//    private static final int THREE_HUNDRED_MSEC = 300;

    private long mPrevPresentUsec;
    private long mPrevMonoUsec;
    private boolean mLoopReset;
    
    private long mFixedFrameDurationUsec;
    private int mFps = 30;
    
    MainHandler mainHandler;
    
    private int smooth_fps_begin;
    private int smooth_fps_end;


    public MySpeedController(MainHandler handler) {
    	mainHandler = handler;
    	smooth_fps_begin = MAX_FPS;
    	smooth_fps_end = NORMAL_FPS;
    	
    	//default: let time stamp itself decide playback speed
    	setPlaybackRate(-1);
    }
    

    /**
     * Sets a fixed playback rate.  
     * If the specified fps > 0, this will ignore the presentation time stamp in the video file.  
	 * If the specified fps <= 0, use the default presentation time stamp in the video file.
     */
    @Override
    public void setPlaybackRate(int fps) {
    	if (fps <= 0) {
    		mFixedFrameDurationUsec = 0;
    	} else if (fps > 0) {
    		mFps = fps;
    		mFixedFrameDurationUsec = ONE_MILLION / fps;
    	}  	
    }
    
	/**
	 *  Added on 01/15/2016 
	 *  Playback rate changes based on time stamps indexes 
	 * 	TODO:
	 *  1. Make the playback speed switches more smoothly.
	 *  2. Switches to larger ranges of frame rate.
	 *  */
	public void setSmartPlaybackRate(int firstIndex, int lastIndex, int currentIndex) {
		if (firstIndex == lastIndex) return;
		/**
		 * high-fps recorded slow motion video playback adjustment
		 * smoother speed change
		 * 2016.4.15
		 */
		//speed up beginning
    	if (currentIndex < firstIndex) {
    		//let frame rate change more smoothly
    		int diff = firstIndex - currentIndex;
    		
    		if (diff < 2000 && diff > 0) {
    		    smooth_fps_begin = smooth_fps_begin - 2; 
    		}
    		
    		if (diff > 2000) smooth_fps_begin = MAX_FPS;
    		
//    		smooth_fps_begin = smooth_fps_begin < 30 ? 30 : smooth_fps_begin;
    		smooth_fps_begin = smooth_fps_begin < NORMAL_FPS ? NORMAL_FPS : smooth_fps_begin;
    		
    		setPlaybackRate(smooth_fps_begin);
    		Log.i(TAG, "begin_diff=" + diff + ", smooth_fps_begin=" + smooth_fps_begin);
    		
    	}
    	
    	//speed up end
    	if (currentIndex >= lastIndex) {
    		//let frame rate change more smoothly
    		int diff = currentIndex - lastIndex;
//    		int smooth_fps = (diff / THREE_HUNDRED_MSEC) * FPS_CHANGE_RATE + NORMAL_FPS;
    		smooth_fps_end = smooth_fps_end + 2;
    		
//    		smooth_fps_end = smooth_fps_end > 240 ? 240 : smooth_fps_end;
    		smooth_fps_end = smooth_fps_end > MAX_FPS ? MAX_FPS : smooth_fps_end;
    		
    		setPlaybackRate(smooth_fps_end);
    		Log.i(TAG, "end_diff=" + diff + ", smooth_fps_end=" + smooth_fps_end);
    	}
    	

//    	//slow motion part
//    	if (currentIndex >= firstIndex && currentIndex < lastIndex) {
//    		int middleIndex = (firstIndex + lastIndex) / 2;
//    		int halfLength = (lastIndex - firstIndex) / 2;
//    		
//    		//smoother frame rate change
//    		if (currentIndex < middleIndex) {
//    			int rate = (middleIndex - currentIndex) / halfLength;
//    			
//    			
//    			if (smooth_fps_middle > 16) {
//    				smooth_fps_middle = smooth_fps_middle - 1;
//    			
//    			} else if (smooth_fps_middle == 16) {
//    				smooth_fps_middle = 15;
//    				offset_tag = middleIndex - currentIndex;
//    				if (offset_tag < 0) offset_tag = 0;
//    			} 
//    			
//    		} else {//currentIndex >= middleIndex
//				int tag = (firstIndex + lastIndex) / 2 + offset_tag;
//    			if (currentIndex < tag) {
//    				smooth_fps_middle = 15;
//    			} else {
//    				smooth_fps_middle = smooth_fps_middle + 1;
//    				if (smooth_fps_middle > 30) smooth_fps_middle = 30;
//    			}
//    				
//    		}
////    	
//    		setPlaybackRate(NORMAL_FPS);
//    	}
    	
	}
    
    //run on decoder thread
	@Override
	public void controlTime(long presentationTimeUsec, long pauseOffset) {		
		// TODO Auto-generated method stub
		if (mPrevMonoUsec == 0) {
            // Latch current values, then return immediately.
            mPrevMonoUsec = System.nanoTime() / 1000;//convert to microsecond unit
            mPrevPresentUsec = presentationTimeUsec;
        } else {
            // Compute the desired time delta between the previous frame and this frame.
            long frameDelta;
            if (mLoopReset) {
                mPrevPresentUsec = presentationTimeUsec - ONE_MILLION / 30;
                mLoopReset = false;
            }
            
            if (mFixedFrameDurationUsec > 0) {
                // Caller requested a fixed frame rate.  Ignore PTS.
                frameDelta = mFixedFrameDurationUsec;
                Log.i(TAG, "FPS=" + ONE_MILLION / frameDelta);
            } else {//use default PTS in the video file
                frameDelta = presentationTimeUsec - mPrevPresentUsec;
                Log.i(TAG, "presentationTimeUsec: " + presentationTimeUsec
                		+ " - " + " mPrevPresentUsec:" + mPrevPresentUsec + " = frameDelta: " + frameDelta
                		+ " FPS=" + ONE_MILLION / (frameDelta > 0 ? frameDelta : 0.001));
            }

      
            if (frameDelta < 0) {
                Log.w(TAG, "Weird, video times went backward");
                frameDelta = 0;
            } else if (frameDelta == 0) {
                // This suggests a possible bug in movie generation.
                Log.i(TAG, "Warning: current frame and previous frame had same timestamp");
            } else {//frameDelta > 0
            	mFps = (int)(ONE_MILLION / frameDelta);
            	mainHandler.setFps(mFps);//update the fps view on Main UI thread
            }


            long desiredUsec = mPrevMonoUsec + frameDelta;  // when we want to wake up
            Log.d(TAG, "desiredUsec: " +  desiredUsec 
            		+ " = " + " mPrevMonoUsec: " +  mPrevMonoUsec + " + frameDelta: " + frameDelta);
            long nowUsec = System.nanoTime() / 1000;
            while (nowUsec < ((desiredUsec - 100) + pauseOffset)) {
                long sleepTimeUsec = (desiredUsec + pauseOffset) - nowUsec;
                try {
                    long startNsec = System.nanoTime();
                    
                    Thread.sleep(sleepTimeUsec / 1000, (int) (sleepTimeUsec % 1000) * 1000);
        
                    long actualSleepNsec = System.nanoTime() - startNsec;
                    Log.d(TAG, "sleep=" + sleepTimeUsec + " actual=" + (actualSleepNsec/1000) +
                                " diff=" + (Math.abs(actualSleepNsec / 1000 - sleepTimeUsec)) +
                                " (usec)");
  
                } catch (InterruptedException ie) {}
                nowUsec = System.nanoTime() / 1000;
            }

            // Advance times using calculated time values, not the post-sleep monotonic
            // clock time, to avoid drifting.
            mPrevMonoUsec += frameDelta;
            mPrevPresentUsec += frameDelta;
        }
	}

	@Override
	public void loopReset() {
		// TODO Auto-generated method stub
		mLoopReset = true;
	}

	private int getFps() {
		return mFps;
	}
	
	
	
	
	
}
