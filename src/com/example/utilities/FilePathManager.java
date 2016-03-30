package com.example.utilities;

import android.os.Environment;
import android.util.Log;

/**
 * Utilities for getting file path.
 * @author xuzebin
 *
 */
public class FilePathManager {
	private static final String TAG = "FilePathManager";
	private FilePathManager(){}
	
	public static String getVideoFileDirectory() {
		String directory = Environment.getExternalStorageDirectory().toString() + 
				"/DCIM/Camera/";
		return directory;
	}
	
	
	public static String getVideoAbsoluteFileName(int width, int height) { 
		if (checkSdCard()) {
			String fileName = getVideoFileDirectory() + "SLOMO_" + width + "x"
					+ height + ".mp4";
			return fileName;
		}
		return null;
	}
	
	
	
	private static boolean checkSdCard() {
		if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
			Log.e(TAG, "SD card not found");
			return false;
		} else {
			return true;
		}
	}
	
}
