package com.example.smartVideoPlayer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;


import android.content.res.AssetFileDescriptor;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Surface;

/**
 * DecoderCore.java
 * Author: xuzebin
 * Created on: 12/03/2016
 *
 * Core components for video decoder. The decoding output goes to surface.
 * This is the improved version of Grafika's.
 * 
 * Added:
 * 1. Seeking.
 * 2. Pause and resume.
 * 3. Interactions with seekBar UI.
 * 
 * Usage: The example can be seen in VideoDecoderThread.java.
 * 1. Create a VideoDecoderCore instance in a separate thread (Must not be used in the Main thread).
 * 2. Initialize the decoder by calling initDecoder().
 * 3. Start decoding by calling startDecoding() (in run() method), 
 * 	it will decode frames as long as surfaceTexture is available.
 * 4. Stop the decoder by calling stop(). 
 * 	(You don't have to call release() to release the resource after decoding is finished, this is already done internally.)
 * 5. Pause or resume the decoder by calling pause() or resume().
 * 6. Seeking to a specific progress by calling seekTo method.
 * 
 * 
 * TODO: 
 * 1. Add audio player and ensure sync with video
 * 2. Bugs to fix:
 * 	2.1 sampleNumber is not accurate when seeking (however, this will not affect playback).
 */
public class VideoDecoder {
	private static final String TAG = "DecoderCore";

	private BufferInfo bufferInfo;
	private File videoFile;
	private AssetFileDescriptor mAssetFileDescriptor;
	private Surface outputSurface;
	private MediaFormat format = null;
    private MediaExtractor extractor = null;
    private MediaCodec decoder = null;
	private int videoWidth;
    private int videoHeight;
    private int duration; //in millisecond
    private int sampleNumber;
    
    protected boolean stopThread = false;
    protected boolean pauseThread = false;
    
    private boolean outputDone;
    private boolean inputDone;
    
    SpeedController controller;//control playback speed
    MainHandler handler;
    
    private long seekedProgress;
    
    private boolean doLoop = true;//default state: keep looping the video.

    /**
     * video playback controller when rendering video frames.
     */
    public interface SpeedController {
    	/**
    	 * Set a fixed playback      
    	 * If the specified fps > 0, this will ignore the presentation time stamp in the video file.  
    	 * If the specified fps <= 0, use the default presentation time stamp in the video file.
    	 * Must be called after the thread starts.
    	 * @param fps the frame rate to set
    	 */
        public void setPlaybackRate(int fps);
        
    	/**
    	 * Called immediately before the frame is rendered.
    	 * @param presentationTimeUsec  The desired presentation time, in microseconds.
    	 * @param pauseOffset the accumulated offset due to pausing
    	 */
        void controlTime(long presentationTimeUsec, long pauseOffset);

        /**
         * Called after the last frame of a looped movie has been rendered.  This allows the
         * callback to adjust its expectations of the next presentation time stamp.
         */
        void loopReset();
    }
    
    /**
     * Constructor for VideoDecoderCore
     * @param videoFile: The video file to demux and decode.
     * @param outputSurface: The decoder decodes the frames to outputSurface.
     * @param controller: Speed controller for frame rate control.
     * @param handler: Handler from main thread for UI updates.
     */
    VideoDecoder(File videoFile_, Surface outputSurface_, SpeedController controller_, MainHandler handler_) {
    	videoFile = videoFile_;
    	outputSurface = outputSurface_;
    	controller = controller_;
    	handler = handler_;
    	sampleNumber = 0;

    }
    
    //When reading video file from res/raw, use AssetFileDescriptor
    VideoDecoder(AssetFileDescriptor assetFileDescriptor_, Surface outputSurface_, SpeedController controller_) {
    	mAssetFileDescriptor = assetFileDescriptor_;
    	outputSurface = outputSurface_;
    	controller = controller_;
    	sampleNumber = 0;

    }

    //Must be called and only need to be called once before startDecoding()
    public void initDecoder() {
    	Log.d(TAG, "preparing decoder...");

    	bufferInfo = new MediaCodec.BufferInfo();
    	
        extractor = new MediaExtractor();
		try {
			extractor.setDataSource(videoFile.toString());//load source from external storage
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        //load video from res/raw/
//		extractor.setDataSource(mAssetFileDescriptor.getFileDescriptor(), 
//				mAssetFileDescriptor.getStartOffset(), mAssetFileDescriptor.getLength());
		
     
        // Select the first video track we find, ignore the rest.
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
            	extractor.selectTrack(i);//select the track
            	
            	Log.d(TAG, "mime: " + mime);

            	videoWidth = format.getInteger(MediaFormat.KEY_WIDTH);
            	videoHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
            	
            	format.setInteger(MediaFormat.KEY_MAX_WIDTH, videoWidth);
            	format.setInteger(MediaFormat.KEY_MAX_HEIGHT, videoHeight);          		
            	
            	setDuration((int)format.getLong(MediaFormat.KEY_DURATION) / 1000);
            	handler.setSeekBarMax(getDuration());//set the video time in Main UI thread
            	          	
            	decoder = MediaCodec.createDecoderByType(mime);
            	
                decoder.configure(format, outputSurface, null, 0); //output goes to surface
//                    decoder.configure(format, null, null, 0);//output does not go to surface
                Log.d(TAG, "format: " + format);
                break;
            }
        }
        
        if (decoder == null) {
        	Log.w(TAG, "video track not found");
        	return;
        }
        
        decoder.start();
        Log.d(TAG, "decoder preparation finish...");    			
    }
    
    public boolean startDecoding() {
    	Log.d(TAG, "begin decoding...");
    	sampleNumber = 0;
    	final int TIMEOUT_USEC = 10000;
        ByteBuffer[] decoderInputBuffers = decoder.getInputBuffers();

        outputDone = false;
        inputDone = false;
        
        long pauseBegin = 0;
        long pauseEnd = 0;
        long pauseOffset = 0;//in microsecond
        
        while (!outputDone) {
        	if (stopThread) {
        		Log.d(TAG, "stop thread");
        		release();
        		return false;
        	}
        	
        	pauseBegin = System.nanoTime();
        	while (pauseThread && !stopThread) {
        		try {
					Thread.sleep(100);//sleep 100 milliseconds each round
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}    		
        	}
        	pauseEnd = System.nanoTime();
        	if (pauseEnd - pauseBegin > 80 * 1000000) {//discard tiny time difference
        		pauseOffset += (pauseEnd - pauseBegin) / 1000;//usec (microsecond)
        	}
    		Log.i(TAG, "pauseOffset: " + pauseOffset / 1000.0 + "ms");
      	
     	
        	//deal with inputBuffer
        	if (!inputDone) {
        		//Retrieve the index of an input buffer to be filled with valid data 
            	//or -1 if no such buffer is currently available.
            	int inputBufIndex = decoder.dequeueInputBuffer(TIMEOUT_USEC);
            	if (inputBufIndex >= 0) {
            		ByteBuffer inputBuf = decoderInputBuffers[inputBufIndex];//retrieve the available buffer to be filled 
            		
            		//Retrieve the current encoded sample 
            		//and store it in the byte buffer starting at the given offset.
                    int sampleSize = extractor.readSampleData(inputBuf, 0);
                    if (sampleSize < 0) {
                    	// End of stream -- send empty frame with EOS flag set.
                        decoder.queueInputBuffer(inputBufIndex, 0, 0, 0L,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                    	//submit it to the codec.
                    	long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufIndex, 0, sampleSize,
                                presentationTimeUs, 0 /*flags*/);
       
                        sampleNumber++;//count the input sample number
                    	extractor.advance();//Advance to the next sample.
                    	
                    	Log.i(TAG, "submitted frame " + sampleNumber + ", size=" + sampleSize + 
                    			", pts(extractor.getsampletime)=" + presentationTimeUs);
                    	
                    	updateUI(presentationTimeUs);
                    }
                    
            	} else {
            		Log.i(TAG, "input buffer not available");
            	}
        	}
     	
        	//deal with outputBuffer
        	if (!outputDone) {
        		//Dequeue an output buffer, block at most TIMEOUT_USEC microseconds.
        		//return the index of an output buffer that has been successfully decoded 
        		int outputBufIndex = decoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);

        		switch(outputBufIndex) {
        		case MediaCodec.INFO_TRY_AGAIN_LATER:
        			Log.d(TAG, "no output from decoder available");
        			break;
        		case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:	
        			Log.d(TAG, "output buffer changed");
        			break;
        		case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
        			Log.d(TAG, "output format changed to " + decoder.getOutputFormat());
        			break;
        		default:
        			if (outputBufIndex < 0) {
        				Log.d(TAG, "outputBufIndex < 0");
        				return false;
        			}

        			Log.i("pts", "bufferinfo.pts = " + bufferInfo.presentationTimeUs);
        			/**
        			 *  Time controller to manage PTS
        			 *  if it is seeking, skip this control
        			 *  
        			 *  Note: isSeeking() will never be called.
        			 *  We play videos whose I_FRAME_INTERVAL = 0 so seeking is accurate
        			 */
        			if (bufferInfo.size != 0 && controller != null && !isSeeking(bufferInfo.presentationTimeUs)) {
        				controller.controlTime(bufferInfo.presentationTimeUs, pauseOffset);//subtract pause time to ensure sync
                    }
        			

        			//--------------------outputBufIndex >= 0---------------------------------
        			//send the buffer to the output surface.
            		//surface will return the buffer to the codec
        			//once the buffer is no longer used 
            		decoder.releaseOutputBuffer(outputBufIndex, true);
            		Log.d(TAG, "send buffer to surface, index=" + outputBufIndex);

        			break;
        		}
        		
        		if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
        			Log.e(TAG, "KEY FRAME: " + sampleNumber +  " at pts : " + 
        						bufferInfo.presentationTimeUs + ", sampleTime: " + extractor.getSampleTime());
        		}
        		  		
        		loopPlayback(doLoop);

        	}
     	
 	
        }
        Log.d(TAG, "decoding end");
        return true;
    }
    
    /* input true if you want to loop the video, or false if not */
    private void loopPlayback(boolean isLoop) {
    	if (isLoop) {
    		if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d(TAG, "loop reset");
                extractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC);                    
                inputDone = false;
                decoder.flush();    // reset decoder state
                controller.loopReset();//reset controller
                sampleNumber = 0;          
            }
    	} else {
    		if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
        		Log.d(TAG, "deocode end --- end of stream");
        		handler.signalEndOfStream();//notify main thread to change button UI
        		stopThread = true;
        		release();
        		
        	}
    	}
    }
    
    /* You must not call it outside this class, since it is already handled internally */
    private void release() {
    	// release resources
        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }
        Log.d(TAG, "resourses released");
    }
    
    public int getWidth() {
    	return videoWidth;
    }
    
    public int getHeight() {
    	return videoHeight;
    }
    
    public int getFrameNumber() {
    	return sampleNumber;
    }
    
    
    //call at the end of decode
    public void dumpVideoInfo() {
    	Log.i(TAG, "VideoWidth=" + videoWidth + ", VideoHeight=" + videoHeight);
    	Log.i(TAG, "Total frame number: " + sampleNumber);
    }
    
    /**
     * Seek to the expected frame
     * @param progress: the expected progress to seek
     * @param preProgress: previous progress before seeking
     */
    public void seekTo(long progress, long preProgress) {
    	final String SEEK = "seek";
    	if (extractor == null) return;
    	
    	seekedProgress = progress;

    	long currentPts = extractor.getSampleTime();
    	
    	/* use this if video frames are all I frames (key frames) */
    	extractor.seekTo(progress * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    	
    	/* use this if video has P or B frames */
//    	extractor.seekTo(progress * 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
    	
    	long afterPts = extractor.getSampleTime();	
    	
    	Log.i(SEEK, "preProgress: " + preProgress + ", expected progress: " + progress + 
    			",  currentPts: " + currentPts + ", afterPts: " + afterPts);
    	
    }
    
    /**
     * This will never be called when I_FRAME_INTERVAL = 0 (configuration for recorder), 
     * which means every frame is I(key) frame, 
     * then seekTo method is precise, and no need to wait
     * @param pts current presentation time stamp
     * @return if current presentation time stamp is matched to the expected time stamp being seeked
     */
    private boolean isSeeking(long pts) {
    	if (pts < seekedProgress) {
    		Log.i(TAG, "is seeking to " + seekedProgress + "...     current pts: " + pts);
    		return true;
    	} else {
    		Log.i(TAG, "seeking end! seek to " + pts);
    		seekedProgress = -1;
    		return false;
    	}
    }

	public int getDuration() {
		Log.i(TAG, "getDuration: " + duration);
		return duration;
	}

	private void setDuration(int mDuration) {
		Log.i(TAG, "setDuration: " + mDuration);
		this.duration = mDuration;
	}

	private void updateCurPosition(long pts) {
		Log.i(TAG, "updateCurPosition----   pts: " + pts + ", pts/1000: " + pts / 1000);
		/* this may be inaccurate when frame rate is variable while playing
		 * but currently we have no other way
		 */
		int progress = (int)pts / 1000;
		
		/** 
		 * The following may never be true, since the last presentation time stamp 
		 * is always about 30ms less than duration retrieved from MediaExtractor.
		 * (Because pts starts from 0) */
		if (progress > this.getDuration()) {
			progress = this.getDuration();
		}
		if (progress < 0) progress = 0;
		
		handler.setSeekBarProgress(progress);//update seekBar progress in Main UI thread
		Log.i("seekBar", "current time (progress): " + progress);
	}
	
	private void updateUI(long pts) {
		int timeStampInMs = (int)pts / 1000;
		handler.setTimeStamp(timeStampInMs);
		handler.getFlowCount(timeStampInMs);
    	updateCurPosition(pts);
	}
	
	
	public void pause() {
		pauseThread = true;
	}
	public void resume() {
		pauseThread = false;
	}
	public void stop() {
		stopThread = true;
	}
	
	public boolean isPause() {
		return pauseThread;
	}
	
	public void setLoop(boolean isLoop) {
		doLoop = isLoop;
	}

	
}