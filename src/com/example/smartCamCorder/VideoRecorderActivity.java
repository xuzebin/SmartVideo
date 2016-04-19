package com.example.smartCamCorder;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.smartVideoPlayer.VideoPlayerActivity;
import com.example.smartvideo.R;
import com.example.utilities.Utils;

/**
 * Video recorder activity.
 * Record the video by encoding preview callback frames 
 * and simultaneously display on SurfaceView.
 * 
 * 
 * @author xuzebin 
 *
 */
public class VideoRecorderActivity extends Activity implements OnClickListener{
    final static String TAG = "VideoRecorderActivity";
//    private Preview mPreview;
//    private MyTextureView mTextureView;
    public TextPreview mTextPreview;
    private Camera mCamera;
    private CameraPreview mPreview;

    
//    private int previewWidth = 1280;//480;//1280;//1920;//
//    private int previewHeight = 720;//320;//720;//1080;//
    
//    private int firstIndex = 0, lastIndex = 0;
  
//    private PreviewHandler mPreviewHandler;
    
    
//	SharedPreferences preferences;
//    SharedPreferences.Editor editor;
    Button recordBtn;
    Button playBtn;
    
    
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {       
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
       
//        mPreview = new Preview(this);
        
//        mTextureView = new MyTextureView(this);  
        
//        mDebugView = (DebugView) findViewById(R.id.camera_debugview);
        mTextPreview = new TextPreview(this);
    
//        mPreviewHandler = new PreviewHandler(this);
        
        
        mPreview = new CameraPreview(this);
        mPreview.setSound(false);//Set sound of recording.
      
        FrameLayout layout = new FrameLayout(this);

        //record button settings
        FrameLayout.LayoutParams recordBtnParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 
        		LayoutParams.WRAP_CONTENT);
        recordBtnParams.gravity = Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM;
        recordBtnParams.bottomMargin = 60;
        recordBtn = new Button(this);
        recordBtn.setBackgroundResource(R.drawable.stop_state);  
        recordBtn.setWidth(200);
        recordBtn.setHeight(200);
        recordBtn.setLayoutParams(recordBtnParams);
        recordBtn.setOnClickListener(this);
        
        //play button settings
        FrameLayout.LayoutParams playBtnParams = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, 
        		LayoutParams.WRAP_CONTENT);
        playBtnParams.gravity = Gravity.RIGHT | Gravity.BOTTOM;
        playBtnParams.bottomMargin = 60;
        playBtnParams.rightMargin = 60;
        playBtn = new Button(this);
        playBtn.setBackgroundResource(R.drawable.playback);  
        playBtn.setWidth(200);
        playBtn.setHeight(200);
        playBtn.setLayoutParams(playBtnParams);

		final Intent intent = new Intent();
		intent.setClass(VideoRecorderActivity.this, VideoPlayerActivity.class);
        playBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				startActivity(intent);
				overridePendingTransition(R.anim.dync_in_from_right, R.anim.dync_out_to_left);
			}
        });
        
        
        layout.addView(mPreview);
        layout.addView(mTextPreview);
        layout.addView(recordBtn, recordBtnParams);
        layout.addView(playBtn, playBtnParams);
        
        setContentView(layout);
        
//        preferences = getSharedPreferences("SmartVideo", Context.MODE_WORLD_READABLE);
//        editor = preferences.edit();
        
        
    }

    /**
     * OnClick listener for record button
     */
	@Override
	public void onClick(View arg0) {
		// TODO Auto-generated method stub
		if (!mPreview.isRecording()) {
    		mPreview.startRecording();
    		recordBtn.setBackgroundResource(R.drawable.record_state);
    		Toast toast = Toast.makeText(getApplicationContext(), "Recording", Toast.LENGTH_SHORT);
    		toast.show();
    		
    	} else {
    		mPreview.stopRecording();
			recordBtn.setBackgroundResource(R.drawable.stop_state);
			Toast toast = Toast.makeText(getApplicationContext(), "Stop", Toast.LENGTH_SHORT);
    		toast.show();
    	}
	}
    
    

	/* save frame indexes and flow count */
//	public void saveFlowCount(int idx, int flow_count) {
//		editor.putInt(Integer.toString(idx), flow_count);
//		Log.i("preferences", "sharedpreferences editor putInt: idx[" + idx + "]= flow_count: " + flow_count);
//	}
	/* save frame time stamps (in millisecond) and flow count */
//	public void saveFlowCount(int time_stamp, int flow_count) {
//		editor.putInt(Integer.toString(time_stamp), flow_count);
//		Log.i("preferences", "sharedpreferences editor putInt: pts[" + time_stamp + "]= flow_count: " + flow_count);
//	}

//    public class Preview extends SurfaceView implements SurfaceHolder.Callback {
//    	private YUVCallback mYUVCallback;
//        private byte[] callbackData;
//        private byte[] callbackData2;
//        private byte[] callbackData3;
//    	
//		public Preview(Context context) {
//			super(context);
//
//	        SurfaceHolder holder = getHolder(); 
//	        holder.setFormat(PixelFormat.TRANSPARENT);
//	        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);  
//	        holder.addCallback(this);
//
//		}
//
//		@Override
//		public void surfaceCreated(SurfaceHolder holder) {
//			Log.i("surface", "surface created");
//
//			startCamera(holder);
//		}
//		@Override
//		public void surfaceDestroyed(SurfaceHolder holder) {
//			Log.i("surface", "surface destroyed");
//			stopCamera();
//		}
//		@Override
//		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//		}
//		
//		public void startCamera(SurfaceHolder holder) {
//			mCamera = Camera.open(0);
//			Parameters p = mCamera.getParameters();
//
//			p.setPreviewSize(Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT);
//			p.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
//			p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
//			p.setPreviewFormat(ImageFormat.NV21);
//			List<String> focusModes = p.getSupportedFocusModes();
//			if (focusModes.contains("continuous-video")) {
//				p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//			}
//			
////			p.setPreviewFpsRange(24000, 30000);
//
//			p.setRecordingHint(true);
//			
//			mCamera.setParameters(p);
//			mCamera.setDisplayOrientation(90);
//			
//			
//
//			callbackData = new byte[Utils.PREVIEW_WIDTH * Utils.PREVIEW_HEIGHT * 3 / 2];
//			callbackData2 = new byte[Utils.PREVIEW_WIDTH * Utils.PREVIEW_HEIGHT * 3 / 2];
////			callbackData3 = new byte[previewWidth * previewHeight * 3 / 2];	
//			
//			try {
//				mCamera.setPreviewDisplay(holder);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			mCamera.startPreview();//start camera preview
//			
//			
//			Utils.printCameraSupportedFpsRange(mCamera);			
//			Utils.printCameraConfigs(mCamera);
//		}
//		public void startRecording() {
//			if (mYUVCallback == null) {
//				mYUVCallback = new YUVCallback();
//			}
//			
//			mCamera.setPreviewCallbackWithBuffer(mYUVCallback);
////			mCamera.setPreviewCallback(mYUVCallback);
////			mCamera.setOneShotPreviewCallback(mYUVCallback);
//			mCamera.addCallbackBuffer(callbackData);
//			mCamera.addCallbackBuffer(callbackData2);
////			mCamera.addCallbackBuffer(callbackData3);
//		}
//		
//		public void stopRecording() {
//			if (mYUVCallback != null) {
//				mYUVCallback.close();
//				mYUVCallback = null;
//			}
//		}
//		public void stopCamera() {
//			stopRecording();
//			if (mCamera != null) {
//				mCamera.setPreviewCallback(null);
//				mCamera.stopPreview();
//				mCamera.release();
//				mCamera = null;
//			}
//		}
//		
//
//	}
//    public class YUVCallback implements PreviewCallback {
//
//    	int testY = 0;
//    	long start = 0;
//    	long end = 0;
//    	long middle = 0;
//    	
//    	int previewCount = 0;
//    	int encodedFrameCount = 0;
//    	int algFrameCount = 0;
//    	long st = 0, en = 0;
//    	
//    	boolean isMotion = false;
//    	int isMotionCount = 0;
//    	
//    	Object object = new Object();//lock for threads
//    	
//    	private byte[] callbackBuffer;//just a reference to the callback buffer
//    	
////    	private VideoEncoderFromBuffer videoEncoder = null;
//    	private VideoEncoder videoEncoder = null;
//    	
//    	
//    	int timeStamp; 
//    	
//    	public YUVCallback() {
//			firstIndex = 0;
//			lastIndex = 0;
//			timeStamp = 0;
//    		videoEncoder = new VideoEncoder(FilePathManager.getVideoAbsoluteFileName(Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT), 
//    									Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT);
//    		
//    		boolean success = videoEncoder.initEncoder();
//    		if (!success) {
//    			Log.e(TAG, "failed to initialize encoder");
//    			return;
//    		}
//    		
//    		startAlgorithmThread();
//
//    	}
//    	void close() {
//    		Log.i("tag", "close called");
//    		videoEncoder.stop();//stop encoder
//    		Log.e("index", "firstIndex: " + firstIndex + ", lastIndex: " + lastIndex);
//    		saveIndexes(firstIndex, lastIndex);//save algorithm indexes
//    	}
//    	
//    	public void saveIndexes(int firstIndex, int lastIndex) {
//    		int indexes[] = SmartNative.getResultIndexes();
//    		int firstIdx = indexes[0];
//    		int lastIdx = indexes[1];
//    		/* if the slow motion part is too short (< 10 frames), extend it to at least 10 frames */
//    		if (lastIdx != firstIdx && lastIdx - firstIdx < 10) {
//    			lastIdx = firstIdx + 10;
//    		}
//    		editor.putInt("firstIndex", firstIdx);
//    		editor.putInt("lastIndex", lastIdx);
//    		editor.commit();
//    		Log.i("preferences", "sharedpreferences editor commit: firstIndex " + firstIdx + ", lastIndex " + lastIdx);
//
//    	}
//       	
//    	//预览回调函数
//		@Override
//		public void onPreviewFrame(byte[] data, Camera camera) {
////			callbackBuffer = data;
//			synchronized (object) {
//				callbackBuffer = data;
//				object.notifyAll();
//			}
//				
//
//				if (previewCount == 0) {
//					st = System.nanoTime() / 1000;
//				}
//				en = System.nanoTime() / 1000;
//	
//				Log.d("timef", "total time: " + (en - st) / 1000.0f + "ms");
//				Log.w("timef", "preview frame count: " + (previewCount++));
//				
//
//			
//				start = System.nanoTime() / 1000;
//				long frame_delta = (start - end);
//				Log.i("timef", "frame delta: " + frame_delta / 1000.0f + "ms");
//	
//				timeStamp = videoEncoder.encodeOneFrame(data);
//				
//	
//				middle = System.nanoTime() / 1000;
//				long encode_time = (middle - start);
//				Log.i("timef", "encode time: " + encode_time / 1000.0f + "ms");
//				
//			
//				//执行绘制
//	//			mDebugView.invalidate();
//	//
//	//			camera.addCallbackBuffer(data);
//	
//	//			
//	//			Log.i("timef", "algorithm+encode time: " + Integer.toString((int)(start-end)) + "ms");
//				end = System.nanoTime() / 1000;
//			
//		}
//		
//		public void startAlgorithmThread() {
//			new Thread(new Runnable() {
//				long algStart = 0, algEnd = 0;
//				long waitStart = 0,  waitEnd= 0;
//
//				@Override
//				public void run() {
//					while (true) {
//						algFrameCount++;
//						waitStart = System.nanoTime() / 1000;
//						synchronized (object) {
//							try {
//								object.wait();
//							} catch (InterruptedException e) {
//								// TODO Auto-generated catch block
//								e.printStackTrace();
//							}
//						}
//						
//						waitEnd = System.nanoTime() / 1000;
//						Log.d("timef", "algorithm wait: " + (waitEnd - waitStart) / 1000.0f + "ms");
//						
//						//optical flow
//						algStart = System.nanoTime() / 1000;
//						//calculate moving points using Optical flow alogrithm
//						int pointCount = SmartNative.calOpticalFlow(callbackBuffer, 
//								Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT, algFrameCount, timeStamp);
//////						
//						
//						algEnd = System.nanoTime() / 1000 - algStart;
//						Log.i("timef", algFrameCount + " algorithm time: " + algEnd / 1000.0f + "ms");
//////						//获取调试信息
//						float debugValue[] = SmartNative.getDebugValue();
////						debugValue[2] = frame_delta;
////						debugValue[3] = encode_time;
////						debugValue[4] = algEnd;
//						Log.e("opticalflow", "flow_count: " + debugValue[11]  + ", flow energy: " + debugValue[8]
//								+ "||  avgMaxBin: " + debugValue[12]
//								+ "maxBin[" + debugValue[9] + "]:" + debugValue[10]);
//
////						//calculate getPointCountFromOpticalFlow() cost time
////						debugValue[8] = System.currentTimeMillis() - end;
//
//						int[] downsizeArr = SmartNative.getDownSize();
//						mPreviewHandler.setDownSize(downsizeArr[0], downsizeArr[1]);
//						
//						mPreviewHandler.setAvgMaxBin(debugValue[12]);
//						
//						mPreviewHandler.setMaxBin(debugValue[10]);
//						mPreviewHandler.setMaxBinIndex((int)debugValue[9]);
////						saveFlowCount(algFrameCount, pointCount);
//						saveFlowCount(timeStamp, pointCount);
//						
//						mPreviewHandler.setFrameNumber(algFrameCount);//update frame number
//						
//						mPreviewHandler.setPointCount(pointCount);
//						mPreviewHandler.setFlowEnergy((int)debugValue[8]);
////						Log.i("opticalflow", "flow energy: " + debugValue[8]);
//						
//						//a hardcode threshold is set here (bad practice)
//						
//						//TODO: consider fps when setting threshold(fps lower, value bigger)
////						if (firstIndex == 0) {//judge the firstIndex
////							if (debugValue[12] > 1000) {//average max bin
//							
//
////							isMotion = true;
////							//exclude camera motion
////							if (debugValue[10] > 500 || pointCount > downsizeArr[0] * downsizeArr[1] / 6
////									|| debugValue[12] > 400) {
////								//camera motion
////								isMotion = false;
////							}
////							
////							//exclude static scene
////							if (debugValue[10] < 80 || pointCount < 80) {
////								//static
////								isMotion = false;
////							}
////							if (isMotion) {//object motion
////								isMotionCount++;
////								if (algFrameCount > 30) {//avoid the initial instability
////									if (firstIndex == 0) {
////										firstIndex = algFrameCount;//record the first motion										
////									}
////																		
////									lastIndex = algFrameCount;//record the last motion
////								}
////							
////							}
//							
//
////							
//								
////							}	
////						}
//					
//
////						//执行绘制
//						mPreviewHandler.invalidateView();
//
//						mCamera.addCallbackBuffer(callbackBuffer);
//											
//					}
//					
//				}
//			
//			}).start();
//		}
//
//    }
    
    
//    class MyTextureView extends TextureView implements TextureView.SurfaceTextureListener {
//    	private static final String tvTAG = "MyTextureView";
//    	long start, end;
//    	public MyTextureView(Context context) {
//			super(context);
//			// TODO Auto-generated constructor stub
//			
//			setSurfaceTextureListener(this);
//		}
//
//		@Override
//		public void onSurfaceTextureAvailable(SurfaceTexture st, int arg1,
//				int arg2) {
//			Log.i(tvTAG, "onSurfaceTextureAvailable");
//			mCamera = Camera.open(0);
//			Parameters p = mCamera.getParameters();
//
//			p.setPreviewSize(Utils.PREVIEW_WIDTH, Utils.PREVIEW_HEIGHT);
//			p.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
//			p.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
//			p.setPreviewFormat(ImageFormat.NV21);
//			List<String> focusModes = p.getSupportedFocusModes();
//			if (focusModes.contains("continuous-video")) {
//				p.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//			}
//			p.setRecordingHint(true);
//			
//			mCamera.setParameters(p);
//			mCamera.setDisplayOrientation(90);
//
//			
//			try {
//				mCamera.setPreviewTexture(st);
//				mCamera.startPreview();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		}
//
//		@Override
//		public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
//			Log.i("surface", "surface destroyed");
//			if (mCamera != null) {
//				mCamera.stopPreview();
//				mCamera.release();
//				mCamera = null;
//				return true;
//			}
//			return false;
//		}
//
//		@Override
//		public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1,
//				int arg2) {
//			// ignore. camera does all the work for us
//			
//		}
//
//		@Override
//		public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
//			// TODO Auto-generated method stub
//			
//			
//			start = System.nanoTime() / 1000;
//			Log.i(TAG, "FRAME: " + (start - end));
//			
//			end = System.nanoTime() / 1000;
//		}
//    	
//    }
    
    

    
//    static class PreviewHandler extends Handler {
//    	/* called in algorithm thread */
//    	private static final int POINT_COUNT = 0;
//    	private static final int DOWNSIZE = 1;
//    	private static final int AVG_MAX_BIN = 2;
//    	private static final int MAX_BIN = 3;
//    	private static final int MAX_BIN_INDEX = 4;
//    	private static final int FLOW_ENERGY = 5;   	
//    	private static final int INVALIDATE_VIEW = 6;
//    	private static final int FRAME_NUMBER = 7;
//    	
//    	
//    	
//    	private WeakReference<VideoRecorderActivity> mWeakActivity;
//    	
//    	public PreviewHandler(VideoRecorderActivity activity) {
//    		mWeakActivity = new WeakReference<VideoRecorderActivity>(activity);
//    	}
//    	
//    	@Override  // runs on UI thread
//        public void handleMessage(Message msg) {
//    		VideoRecorderActivity activity = mWeakActivity.get();
//    		if (activity == null) {
//                Log.w(TAG, "PreviewHandler.handleMessage: weak ref is null");
//                return;
//            }
//    		
//    		switch(msg.what) {
//    		case POINT_COUNT:
//    			activity.mDebugView.setPointCount(msg.arg1);
//    			Log.i("debug", msg.arg1 + "");
//    			break;
//    		case DOWNSIZE:
//    			activity.mDebugView.setDownsize(msg.arg1, msg.arg2);
//    			Log.i("debug", msg.arg1 + "");
//    			break;
//    		case AVG_MAX_BIN:
//    			float avg_maxbin = msg.arg1 + msg.arg2 / 1000.0f;
//    			activity.mDebugView.setAvgMaxBin(avg_maxbin);
//    			break;
//    		case MAX_BIN:
//    			float maxbin = msg.arg1 + msg.arg2 / 1000.0f;
//    			activity.mDebugView.setMaxBin(maxbin);
//    			break;
//    		case MAX_BIN_INDEX:
//    			activity.mDebugView.setMaxBinIndex(msg.arg1);
//    			break;
//    		case FRAME_NUMBER:
//    			activity.mDebugView.setFrameNumber(msg.arg1);
//    			break;
//    		case FLOW_ENERGY:
//    			activity.mDebugView.setFlowEnergy(msg.arg1);
//    			break;
//    		case INVALIDATE_VIEW:
//    			activity.mDebugView.invalidate();
//    			break;
//    		default:
//                throw new RuntimeException("unknown msg " + msg.what);
//    		}	
//    	}
//    	
//    	public void setPointCount(int pointCount) {
//    		sendMessage(obtainMessage(POINT_COUNT, pointCount, 0));
//    		Log.i("debug", "set pointcount: " + pointCount);
//    	}
//    	public void setDownSize(int width, int height) {
//    		sendMessage(obtainMessage(DOWNSIZE, width, height));
//    	}
//    	
//    	public void setAvgMaxBin(float avgMaxBinValue) {
//    		int avgMaxBinValueInt = (int)avgMaxBinValue;
//    		sendMessage(obtainMessage(AVG_MAX_BIN, avgMaxBinValueInt, 
//    				(int)(avgMaxBinValue - avgMaxBinValueInt) * 1000));
//    	}
//    	
//    	public void setMaxBin(float maxBinValue) {
//    		int maxBinValueInt = (int)maxBinValue;
//    		sendMessage(obtainMessage(MAX_BIN, maxBinValueInt, 
//    				(int)(maxBinValue - maxBinValueInt) * 1000));
//    	}
//    	
//    	public void setMaxBinIndex(int index) {
//    		sendMessage(obtainMessage(MAX_BIN_INDEX, index, 0));
//    	}
//    	
//    	public void setFrameNumber(int frame) {
//    		sendMessage(obtainMessage(FRAME_NUMBER, frame, 0));
//    	}
//    	
//    	public void setFlowEnergy(int flowEnergy) {
//    		sendMessage(obtainMessage(FLOW_ENERGY, flowEnergy, 0));
//    	}
//    	
//    	public void invalidateView() {
//    		sendEmptyMessage(INVALIDATE_VIEW);
//    	}
//    	
//    }






}
