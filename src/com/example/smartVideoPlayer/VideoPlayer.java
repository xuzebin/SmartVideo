package com.example.smartVideoPlayer;

import java.io.File;

import android.util.Log;
import android.view.Surface;


/**
 * VideoPlayer.java
 * A separate thread for decoder.
 * Handle interactions between VideoDecoderCore and VideoPlayerActivity.
 * Author: xuzebin
 * Created on: 12/07/2016
 * 
 * Updated (01/12/2016): Change the relationship from is-a to has-a. 
 * 						 Own the DecoderCore instance itself rather than inheriting from it.
 * 
 * Updated on 04/20/2016: Rename VideoDecoderThread to VideoPlayer, VideoDecoderCore to VideoDecoder.
 * 						  Create VideoDecoder instance inside VideoPlayer class rather than initializing it outside.
 */

public class VideoPlayer implements Runnable {
	private static final String TAG = "DecoderCore";
	private static final long ONE_MILLION = 1000000L;
	VideoDecoder decoder;

	public VideoPlayer(VideoDecoder decoder) {
		this.decoder = decoder;
		
		decoder.initDecoder();
	}
	public VideoPlayer(File selectedFile, Surface surface, MySpeedController controller, MainHandler handler) {
		decoder = new VideoDecoder(selectedFile, surface, controller, handler);
		decoder.initDecoder();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		long start = System.nanoTime();
				
		decoder.startDecoding();	//core components for decoding
		Log.i(TAG, "decode time: " + (System.nanoTime() - start) / ONE_MILLION + "ms");			
		
		decoder.dumpVideoInfo();
	}
	
	public void startPlaying() {
		new Thread(this, "MyDecoder").start();
		Log.d(TAG, "thread start");
	}
	
	public void stopPlaying() {
		decoder.stop();
	}
	
	public void pause() {
		decoder.pause();
	}
	public void resume() {
		decoder.resume();
	}
	
	public boolean isPause() {
		return decoder.isPause();
	}
	
	
	public void seekTo(long progress, long preProgress) {
		if (decoder != null) {
			decoder.seekTo(progress, preProgress);
		}
	}
	
	public int getWidth() {
		return decoder.getWidth();
	}
	
	public int getHeight() {
		return decoder.getHeight();
	}
	
	public void setLoop(boolean isLoop) {
		decoder.setLoop(isLoop);
	}
}
