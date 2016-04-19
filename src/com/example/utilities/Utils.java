package com.example.utilities;

import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;
/**
 * Some utilities handling formats, output of smartphone configurations,
 * and constant values.
 * 
 * @author xuzebin
 *
 */
public class Utils {
	private static final String TAG = "Utils";
	private static final int SEC_IN_ONE_MIN = 60;
	
	private Utils(){}
	
	//camera preview size
	public static int PREVIEW_WIDTH = 1024;//320;//640;//1024;//1280;
	public static int PREVIEW_HEIGHT = 768;//240;//480;// 768;//720;
	
	public static void setPreviewSize(int width, int height) {
		PREVIEW_WIDTH = width;
		PREVIEW_HEIGHT = height;			
	}
	
	/**
	 * Get the string of formatted time to display.
	 * @param milliseconds total time of video
	 * @return formatted time of video
	 */
	public static String formatTime(long milliseconds) {
		int seconds = (int)milliseconds / 1000;
		int minute_part = 0;
		if (seconds >= SEC_IN_ONE_MIN) {
			minute_part = seconds / SEC_IN_ONE_MIN;
		}
		if (minute_part >= SEC_IN_ONE_MIN) {
			Log.e(TAG, "Video time exceeds 60 minutes");
			return null;
		}
		
		String minute_text = minute_part >= 10 ? Integer.toString(minute_part) : ("0" + minute_part);
		
		int second_part = seconds % SEC_IN_ONE_MIN;
		String second_text = second_part >= 10 ? Integer.toString(second_part) : ("0" + second_part);
	
		String time = minute_text + ":" + second_text;
		
		return time;
	}
	
	/**
	 * check if the codec support adaptive playback & seeking
	 * @param codec
	 * @param mime_type
	 * @return true if it supports
	 */
	public static boolean isAdaptivePlaybackSupported(MediaCodec codec, String mime_type) {
        MediaCodecInfo codecInfo = codec.getCodecInfo();
        CodecCapabilities codecCapabilites = codecInfo.getCapabilitiesForType(mime_type);
        
    	boolean isSupported = codecCapabilites.isFeatureSupported(CodecCapabilities.FEATURE_AdaptivePlayback);
    	Log.e(TAG, "isAdaptivePlaybackSupported=" + isSupported);
		return isSupported;
	}
	
	/**
	 * Output all supported built-in codecs on the tested smartphone.
	 */
	public static void printSupportedBuiltInCodecs() {
		//iterate built-in codecs
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
			String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				Log.i(TAG, types[j]);
			}		
		}
	}
	

	/**
	 * Output a specified camera's configurations
	 * @param which: 0 represents back, 1 front
	 */
	public static void printCameraConfigs(Camera mCamera) {
		Parameters params = mCamera.getParameters();
		Log.i(TAG, params.flatten());
	}
	
	/**
	 * Get and print a list of supported frame rate range of the specififed camera.
	 * @param mCamrea
	 */
	public static List<int[]> printCameraSupportedFpsRange(Camera mCamera) {
		Parameters params = mCamera.getParameters();
		int min_fps, max_fps;
		List<int[]> fpsRangeList = params.getSupportedPreviewFpsRange();
		
		//output
		for (int[] fpsRange : fpsRangeList) {
			min_fps = fpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
			max_fps = fpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
			Log.i(TAG, "min_fps: " + ((float)(min_fps / 1000)) + " , max_fps" + ((float)(max_fps / 1000)));
			
		}
		
		return fpsRangeList;
	}
	
	/**
	 * Output phone's information
	 */
    public static void printPhoneConfigs(Context context) {
    	TelephonyManager phoneMgr = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    	String phoneModel = Build.MODEL;
    	String phoneNumber = phoneMgr.getLine1Number();
    	String SDKVersion = Build.VERSION.SDK;
    	String OSVersion = Build.VERSION.RELEASE;
    	String deviceID = phoneMgr.getDeviceId();
    	String deviceSWVersion = phoneMgr.getDeviceSoftwareVersion();
    	
    	Log.i(TAG, "phone Model: " + phoneModel);
    	Log.i(TAG, "phone Number: " + phoneNumber);
    	Log.i(TAG, "SDK Version: " + SDKVersion);
    	Log.i(TAG, "OS Version: " + OSVersion);
    	Log.i(TAG, "device ID: " + deviceID);
    	Log.i(TAG, "device SWVersion: " + deviceSWVersion);
    }
	
	
}
