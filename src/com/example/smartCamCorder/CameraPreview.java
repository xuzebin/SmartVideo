package com.example.smartCamCorder;

import java.io.IOException;
import java.util.List;

import com.example.utilities.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaActionSound;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Camera preview using SurfaceView.
 * Camera will open as soon as surface is created 
 * and will close when surface is destroyed
 * 
 * Usage:
 * 1. Create a CameraPreview instance in main thread and add it to layout
 * 2. Call startCamera(surfaceHolder) once at the beginning to open the camera.
 * 3. Call startRecording() to record the video.
 * 4. Call stopRecording() to stop recording the video.
 * 5. Call stopCamera() to stop the camera.
 * 
 * @author xuzebin
 *
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "CameraPreview";
	
	SurfaceHolder holder;
	Camera mCamera;
	CamPreviewController mCamPreviewController = null;
	TextPreviewHandler mTextPreviewHandler;
	
    private byte[] callbackData;
    private byte[] callbackData2;
	SharedPreferences mPreferences;
	
	MediaActionSound mediaActionSound;//control sound of when clicking record and stop.
	private boolean openSound;
	
	public CameraPreview(Context context) {
		super(context);
		mPreferences = context.getSharedPreferences("SmartVideo", Context.MODE_WORLD_READABLE);	
		mTextPreviewHandler = new TextPreviewHandler((VideoRecorderActivity)context);
		holder = this.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);  
        holder.addCallback(this);
				
		callbackData = new byte[Utils.PREVIEW_WIDTH * Utils.PREVIEW_HEIGHT * 3 / 2];
		callbackData2 = new byte[Utils.PREVIEW_WIDTH * Utils.PREVIEW_HEIGHT * 3 / 2];
		
		mediaActionSound = new MediaActionSound();
		mediaActionSound.load(MediaActionSound.START_VIDEO_RECORDING);
		mediaActionSound.load(MediaActionSound.STOP_VIDEO_RECORDING);
		
		openSound = true;//default: open sound of recording
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub
		startCamera(holder);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		stopCamera();
	}
	
	public void startCamera(SurfaceHolder holder) {
		try {
			mCamera = Camera.open(0);
//			
//			Camera.Size size = mCamera.getParameters().getPreviewSize();
//			Utils.setPreviewSize(size.width, size.height);
			
			Parameters p = mCamera.getParameters();
			p.setPreviewSize(Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT);
			p.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
			p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
			p.setPreviewFormat(ImageFormat.NV21);
//			p.setPreviewFormat(ImageFormat.YV12);
			List<String> focusModes = p.getSupportedFocusModes();
			if (focusModes.contains("continuous-video")) {
				p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			}
			
//			p.setPreviewFpsRange(1000, 120000);

			/**
			 * This is a hint to make frame rate from preview callback higher.
			 */
			p.setRecordingHint(true);
			
			mCamera.setParameters(p);
			mCamera.setDisplayOrientation(90);

			mCamera.setPreviewDisplay(holder);
			
			//print some information of the camera (just for test)
			Utils.printCameraSupportedFpsRange(mCamera);
			Utils.printCameraConfigs(mCamera);
			
		} catch (IOException e) {
            e.printStackTrace();
        }
		
		mCamera.startPreview();
	}
	
	public void stopCamera() {
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
		}
	}
	
	public void startRecording() {
		
		if (openSound) mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
		
		if (mCamPreviewController == null) {		
			mCamPreviewController = new CamPreviewController(mPreferences, mTextPreviewHandler);
		}
		
		mCamera.setPreviewCallbackWithBuffer(mCamPreviewController);
		mCamera.addCallbackBuffer(callbackData);
		mCamera.addCallbackBuffer(callbackData2);
	}
	
	public void stopRecording() {
		
		if (openSound) mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
		
		if (mCamPreviewController != null) {
			mCamPreviewController.close();
			mCamPreviewController = null;
		}
		if (mCamera != null) {
			mCamera.setPreviewCallback(null);
		}
	}
	
	
	/**
	 * check if camera is recording video.
	 */
	public boolean isRecording() {
		return mCamPreviewController != null;
	}
	
	/**
	 * set recording sound
	 * @param mOpenSound true to open, false to shut down.
	 */
	public void setSound(boolean mOpenSound) {
		openSound = mOpenSound;
	}
  

}
