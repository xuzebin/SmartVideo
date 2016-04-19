package com.example.smartCamCorder;

import com.example.smartNative.SmartNative;
import com.example.utilities.FilePathManager;
import com.example.utilities.Utils;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaActionSound;
import android.util.Log;

/**
 * This class has 2 threads: 
 * One for encoder in onPreviewFrame callback,
 * another for algorithms calculation.
 * 
 * Encoder thread:
 * Encode frames from preview callback.
 * 
 * Algorithm thread:
 * Calculate optical flow and extract features from it in another thread, 
 * implemented in native layer with OpenCV.
 * 
 * Synchronization:
 * When algorithm calculation for each frame is done, 
 * add callback buffer back in order for encoder thread to receive a new frame in onPreviewFrame().
 * In turn, when a new frame arrives in onPreviewFrame(), call notify() in
 * the encoder thread to notify algorithm thread to do calculations.
 * 
 * @author xuzebin
 *
 */
public class CamPreviewController implements PreviewCallback {
	private static final String TAG = "CamPreviewController";
	
	//time calculations
	private long start = 0;
	private long end = 0;
	private long middle = 0;
	
	//frame count
	private int previewCount = 0;
	private int algFrameCount = 0;
	
	private long st = 0, en = 0;
	
	private Object object = new Object();//lock for threads
	
	private byte[] callbackBuffer;//a reference to the callback buffer
	private byte[] grayData = new byte[Utils.PREVIEW_WIDTH * Utils.PREVIEW_HEIGHT];

	//core components of video encoder
	private VideoEncoder videoEncoder = null;
		
	private int timeStamp;
	
	private int firstIndex;//The time stamp of the first frame for slow-motion
	private int lastIndex;//The time stamp of the last frame for slow-motion
	
	public SharedPreferences mPreferences;
	public SharedPreferences.Editor mEditor;
	
	public TextPreviewHandler mTextPreviewHandler;
	Camera mCamera = null;
	
	
	public CamPreviewController(SharedPreferences preferences, TextPreviewHandler previewHandler) {
		mPreferences = preferences;
		mEditor = mPreferences.edit();
		mTextPreviewHandler = previewHandler;
		firstIndex = 0;
		lastIndex = 0;
		timeStamp = 0;
		videoEncoder = new VideoEncoder(FilePathManager.getVideoAbsoluteFileName(Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT), 
									Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT);
		
		
		boolean success = videoEncoder.initEncoder();
		if (!success) {
			Log.e(TAG, "failed to initialize encoder");
			return;
		}
		
		startAlgorithmThread();

	}
	
	void close() {
		videoEncoder.stop();//stop encoder
		mTextPreviewHandler.signalEnd();//tell the main thread recording ends and reset UIs.
		Log.e("index", "firstIndex: " + firstIndex + ", lastIndex: " + lastIndex);
		saveIndices(firstIndex, lastIndex);//save algorithm indexes
		commitSavings();
	}
	
	private void saveIndices(int firstIndex, int lastIndex) {  
		//get the indexes calculated in native layer.
		int indexes[] = SmartNative.getResultIndexes();
		
		int firstIdx = indexes[0];
		int lastIdx = indexes[1];
		
		/* if the slow motion part is too short (0 < slow motion < 30 frames),
		 *  extend it to at least 10 frames */
		if (lastIdx != firstIdx && lastIdx - firstIdx < 30) {
			lastIdx = firstIdx + 30;
		}
		mEditor.putInt("firstIndex", firstIdx);
		mEditor.putInt("lastIndex", lastIdx);
		Log.i(TAG, "sharedpreferences editor putInt: firstIndex " + firstIdx + ", lastIndex " + lastIdx);
	}
	
	/* save frame time stamps (in millisecond) and flow count */
	private void saveFlowCount(int time_stamp, int flow_count) {
		mEditor.putInt(Integer.toString(time_stamp), flow_count);
		Log.i(TAG, "sharedpreferences editor putInt: pts[" + time_stamp + "]= flow_count: " + flow_count);
	}
	
	/**
	 * Must call this at the end to commit all savings into file 
	 * We do not call this frequently because this I/O operation costs time 
	 */
	private void commitSavings() {
		mEditor.commit();
		Log.i(TAG, "sharedpreferences: data committed.");
	}
   	
	//preview callback function
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		mCamera = camera;
		synchronized (object) {
			callbackBuffer = data;
			object.notifyAll();
		}
			

		if (previewCount == 0) {
			st = System.nanoTime() / 1000;
		}
		en = System.nanoTime() / 1000;

		Log.d("timef", "total time: " + (en - st) / 1000.0f + "ms");
		Log.w("timef", "preview frame count: " + (previewCount++));
			
		
		start = System.nanoTime() / 1000;
		long frame_delta = (start - end);
		Log.i("timef", "frame delta: " + frame_delta / 1000.0f + "ms");

		/* encode preview data here */
		timeStamp = videoEncoder.encodeOneFrame(data);
			

		middle = System.nanoTime() / 1000;
		long encode_time = (middle - start);
		Log.i("timef", "encode time: " + encode_time / 1000.0f + "ms");
			
		end = System.nanoTime() / 1000;
		
	}
	
	private void startAlgorithmThread() {
		new Thread(new Runnable() {
			long algStart = 0, algEnd = 0;
			long waitStart = 0,  waitEnd= 0;

			@Override
			public void run() {
				while (true) {
					algFrameCount++;
					waitStart = System.nanoTime() / 1000;
					
					/* wait until a frame is available (when preview callback is called) */
					synchronized (object) {
						try {
							object.wait();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					
					waitEnd = System.nanoTime() / 1000;
					Log.d("timef", "algorithm wait: " + (waitEnd - waitStart) / 1000.0f + "ms");
					

					algStart = System.nanoTime() / 1000;
					
					
					//convert yuv data to gray data
					NV21toGray(callbackBuffer, grayData, Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT);
					
					
					//calculate moving points using Optical flow algorithm
					int pointCount = SmartNative.calOpticalFlow(grayData, 
							Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT, algFrameCount, timeStamp);
					
					algEnd = System.nanoTime() / 1000 - algStart;
			
					Log.i("timef", algFrameCount + " algorithm time: " + algEnd / 1000.0f + "ms");
    				
					
					//get debug values
					float debugValue[] = SmartNative.getDebugValue();

					Log.e("opticalflow", "flow_count: " + debugValue[11]  + ", flow energy: " + debugValue[8]
							+ "||  avgMaxBin: " + debugValue[12]
							+ "maxBin[" + debugValue[9] + "]:" + debugValue[10]);


					int[] downsizeArr = SmartNative.getDownSize();
					mTextPreviewHandler.updateDownSize(downsizeArr[0], downsizeArr[1]);
					mTextPreviewHandler.updateAvgMaxBin(debugValue[12]);
					mTextPreviewHandler.updateMaxBin(debugValue[10]);
					mTextPreviewHandler.updateMaxBinIndex((int)debugValue[9]);		
					mTextPreviewHandler.updateFrameNumber(algFrameCount);//update frame number			
					mTextPreviewHandler.updatePointCount(pointCount);
					mTextPreviewHandler.updateFlowEnergy((int)debugValue[8]);
					mTextPreviewHandler.updateTimeStamp(timeStamp);
					
					//update view
					mTextPreviewHandler.invalidateView();
					
					saveFlowCount(timeStamp, pointCount);
					
					/* Add the callback buffer here to ensure synchronization */
					if (mCamera != null) {
						mCamera.addCallbackBuffer(callbackBuffer);
					}
													
				}
				
			}
		
		}).start();
	}

	private void NV21toGray(byte[] nv21bytes, byte[] gray,
			int width, int height) {
		System.arraycopy(nv21bytes, 0, gray, 0, width * height);
		for (int i = 0; i < width * height; ++i) {
			gray[i] = nv21bytes[i];
		}
	}

}
