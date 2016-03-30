package com.example.smartVideoPlayer;

import android.util.Log;


/**
 * DecoderThread.java
 * A separate thread for decoder.
 * Handle interactions between VideoDecoderCore and VideoPlayerActivity.
 * Author: xuzebin
 * Created on: 12/07/2016
 * 
 * Updated (01/12/2016): Change the relationship from is-a to has-a. 
 * 						 Own the DecoderCore instance itself rather than inheriting from it.
 */

public class VideoDecoderThread implements Runnable {
	private static final String TAG = "DecoderCore";
	private static final long ONE_MILLION = 1000000L;
	VideoDecoderCore decoderCore;

	public VideoDecoderThread(VideoDecoderCore decoderCore) {
		this.decoderCore = decoderCore;
		
		decoderCore.initDecoder();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		long start = System.nanoTime();
				
		decoderCore.startDecoding();	//core components for decoding
		Log.i(TAG, "decode time: " + (System.nanoTime() - start) / ONE_MILLION + "ms");			
		
		decoderCore.dumpVideoInfo();
	}
	
	public void startPlaying() {
		new Thread(this, "MyDecoder").start();
		Log.d(TAG, "thread start");
	}
	
	public void stopPlaying() {
		decoderCore.stop();
	}
	
	public void pause() {
		decoderCore.pause();
	}
	public void resume() {
		decoderCore.resume();
	}
	
	public boolean isPause() {
		return decoderCore.isPause();
	}
	
	
	public void seekTo(long progress, long preProgress) {
		if (decoderCore != null) {
			decoderCore.seekTo(progress, preProgress);
		}
	}
	
	public int getWidth() {
		return decoderCore.getWidth();
	}
	
	public int getHeight() {
		return decoderCore.getHeight();
	}
	
	public void setLoop(boolean isLoop) {
		decoderCore.setLoop(isLoop);
	}
}
