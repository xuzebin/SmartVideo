package com.example.smartVideoPlayer;

import java.io.File;
import java.lang.ref.WeakReference;

import com.example.smartvideo.R;

import com.example.utilities.FilePathManager;
import com.example.utilities.MiscUtils;
import com.example.utilities.Utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.view.TextureView;

/**
 * Video player activity.
 * Play the video by decoding video on surface, and display on TextureView.
 * @author xuzebin
 *
 * Features:
 * 1. Touch screen to pause and touch it again to resume.
 * 2. Seek to any frame at any time(playing and pausing) by dragging seekBar.
 * 3. Switch speed at any time.
 *
 * TODO: 
 * 1. Bugs to fix:
 * 		1.1 (fixed)Touch the screen after clicking stop button and before replaying the video. 
 * 		1.2 (fixed)Touch the screen at the initial state before clicking "play" button to play the video.
 * 		1.3 Playback looping delays at the beginning sometimes.
 * 2. Use ID instead of text name as an identifier in button listener.
 * 3. Make speed switching smoother.
 * 4. (01/15/2016: Done)Separate the speed switching part into a class/function.
 * 5. (01/15/2016: Done)Separate MainHandler into a class.
 */
public class VideoPlayerActivity extends Activity implements TextureView.SurfaceTextureListener, 
													OnItemSelectedListener {
	private static final String TAG = "VideoPlayerActivity";
	
	private static final int DEFAULT_FRAME_RATE = -1;//default
	private static String fileDirectory;
	
	private File selectedFile = null;
	private String[] mVideoFiles;

	
	/* here we use TextureView to display the decoded video */
	private TextureView mTextureView;
	/**
	 * However, we can use SurfaceView to display video instead. 
	 * But TextureView is more flexible in some cases.
	 */
//	private SurfaceView mSurfaceView;
	
	Surface surface;
	SurfaceTexture surfaceTexture;

	private VideoPlayer videoPlayer = null;
	MySpeedController controller;
	
	/* UIs */
	Button play_stop_btn;
	Switch switch_view;
	TextView time_stamp_view;
	TextView fps_view;
	TextView firstIdx_view;
	TextView lastIdx_view;
	TextView flowcount_view;
	RadioGroup fpsGroup;
	SeekBar seekBar;
	SeekBarListener seekBarListener;
	TextView start_time_view;
	TextView end_time_view;
	RadioButton button_10fps;
	RadioButton button_30fps;
	RadioButton button_60fps;
	
	
	private MainHandler mHandler;//update frame number, fps...
	
	private int selectedFPS;//play the video based on this frame rate
	/* motion detection results read from the file */
	private static int firstIndex = 0;
	private static int lastIndex = 0;
	private SharedPreferences preferences;
	
	private static boolean autoMode;
	
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
        
		requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		super.onCreate(savedInstanceState);
	
		setContentView(R.layout.activity_video_player);
		
		fileDirectory = FilePathManager.getVideoFileDirectory();
		
		selectedFPS = DEFAULT_FRAME_RATE;//default frame rate
		
		preferences = this.getSharedPreferences("SmartVideo", Context.MODE_WORLD_READABLE);

		initUIs();//all the UI initializations	
        
        autoMode = true;//default mode
       	
	}
	
	private void initUIs() {	
		
        initFileSelector(fileDirectory);//a spinner to select files under a specified directory
        
        
		/**
		 * initializations when using surfaceview to display 
		 */
//		mSurfaceView = (SurfaceView)findViewById(R.id.play_video_on_surface);
////		mSurfaceView.setRotation(90.0f);
//		SurfaceHolder holder = mSurfaceView.getHolder();
//		holder.addCallback(this);
        
        switch_view = (Switch) findViewById(R.id.mode_switch);
        play_stop_btn = (Button) findViewById(R.id.play_stop_btn);
//        time_stamp_view = (TextView) findViewById(R.id.timeStamp);
        fps_view = (TextView) findViewById(R.id.display_fps);
        firstIdx_view = (TextView) findViewById(R.id.firstIdx);
        lastIdx_view = (TextView) findViewById(R.id.lastIdx);
//        flowcount_view = (TextView) findViewById(R.id.display_flowcount);
        start_time_view = (TextView) findViewById(R.id.startTime);
        end_time_view = (TextView) findViewById(R.id.endTime);
        fpsGroup = (RadioGroup) findViewById(R.id.select_fps);
        seekBar = (SeekBar) findViewById(R.id.timeSeekBar);
        button_10fps = (RadioButton) findViewById(R.id.fps10);
        button_30fps = (RadioButton) findViewById(R.id.fps30);
        button_60fps = (RadioButton) findViewById(R.id.fps60);
        
        //initially set to invisible
        button_10fps.setEnabled(false);
        button_30fps.setEnabled(false);
        button_60fps.setEnabled(false);
        
        
        mTextureView = (TextureView) findViewById(R.id.video_on_textureview);
        mTextureView.setSurfaceTextureListener(this);
        mTextureView.setRotation(90.0f);
        

        /**
         *  OnTouch listener for video pausing and resuming
         *  TODO: Bugs to fix: (crash)
         *  1. Touch the screen after clicking stop button and before replaying the video.
         *  2. Touch the screen at the initial state before clicking "play" button to play the video.
         **/
        mTextureView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub	
				if (arg1.getAction() == MotionEvent.ACTION_DOWN) {				
					if (!videoPlayer.isPause()) {
						//pause the video
						videoPlayer.pause();			
						Toast toast = Toast.makeText(getApplicationContext(), "Pause", Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
					} else {
						videoPlayer.resume();
						Toast toast = Toast.makeText(getApplicationContext(), "Resume", Toast.LENGTH_SHORT);
						toast.setGravity(Gravity.CENTER, 0, 0);
						toast.show();
					}				
				}
				return true;
			}
        	
        });
        
        /**
         * Switch view listener to ensure when autoMode is on,
         * we cannot manually change the frame rate using the radio buttons.
         */
        switch_view.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton btn, boolean isChecked) {
				// TODO Auto-generated method stub
				autoMode = isChecked;
				if (isChecked) {
					//auto mode
					button_10fps.setEnabled(false);
					button_30fps.setEnabled(false);
					button_60fps.setEnabled(false);
							      		        
				} else {
					//manual mode
					button_10fps.setEnabled(true);
					button_30fps.setEnabled(true);
					button_60fps.setEnabled(true);
		            changeFPS(DEFAULT_FRAME_RATE);
		            
				}
				
		        /**
		         * get detection results from the file(SharedPreferences), 
		         * must be called before getting a SharedPreferences instance.
		         * param true in smart(auto) mode, false in manual mode
		         **/
		        setIndexesView(isChecked);
			}
     
        });
        
        /**
         * users can set a selected frame rate at any time during or prior to video playback
         */
        fpsGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				// TODO Auto-generated method stub
				int radioBtnId = arg0.getCheckedRadioButtonId();
				RadioButton rbtn = (RadioButton) findViewById(radioBtnId);
				if (rbtn.getText().equals("10fps")) {
					selectedFPS = 10;
				} else if (rbtn.getText().equals("30fps")) {
					selectedFPS = 30;
				} else if (rbtn.getText().equals("60fps")) {
					selectedFPS = 60;
				}
				changeFPS(selectedFPS);				
			}
        
        });
        
        /**
         * seekBar listener for video playback progress,
         * including seeking at any frame by moving the seekBar
         */
        seekBarListener = new SeekBarListener();
        seekBar.setOnSeekBarChangeListener(seekBarListener);
        
        
        /**
         * get detection results from the file(SharedPreferences), 
         * must be called before getting a SharedPreferences instance.
         * param true in smart(auto) mode, false in manual mode
         **/
        setIndexesView(true);

        
        //disable some UI before playing the video to avoid bugs.
        updateUI(false);
        
	}
	
    @Override  
    protected void onResume() {
        Log.d(TAG, " onResume");
        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, " onPause");
        super.onPause();
    }
	
	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}
	
	@Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
		if (videoPlayer != null) {
			videoPlayer.stopPlaying();
			videoPlayer = null;
		}
    }
	
	private void initFileSelector(String directory) {
		//select video file
		Spinner fileSpinner = (Spinner) findViewById(R.id.select_file);
		mVideoFiles = MiscUtils.getFiles(new File(directory), "*.mp4");	
		
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, mVideoFiles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        fileSpinner.setAdapter(adapter);
        fileSpinner.setOnItemSelectedListener(this);
        
	}
	
    /**
     * OnClick button controlling playing and stopping 
     * (different from pausing and resuming)
     * TODO: Judge using button text content is not the best practice, 
     * 		 need to use ID as an identifier
     */
    public void onclick_play_stop(View argu) {
    	if (play_stop_btn.getText().equals("Play")) {
    		stopVideo();
    		updateUI(true);
    		startVideo();
    		play_stop_btn.setText("Stop");
    	} else if (play_stop_btn.getText().equals("Stop")) {
    		stopVideo();
    		updateUI(false);
    		play_stop_btn.setText("Play");
    	}    	
    }
	
	/**
	 * Read from shared preferences to get the detection results
	 * obtained in the recoding stage and set them to display on views.
	 * 
	 * @param isSmartMode if in smart mode, set index view visible, else set index view invisible.
	 */
	public void setIndexesView(boolean isSmartMode) {
		if (isSmartMode) {
			if (preferences.contains("firstIndex")) {
				firstIndex = preferences.getInt("firstIndex", 0);
//				firstIndex = 7083;
				firstIdx_view.setText("first index: " + (firstIndex / 1000.0) + " s");
			}
			if (preferences.contains("lastIndex")) {
				lastIndex = preferences.getInt("lastIndex", 0);
//				lastIndex = 15230;
				lastIdx_view.setText("last index: " + (lastIndex / 1000.0) + " s");
			}
			
			Log.i("index", "firstIndex: " + firstIndex + ", lastIndex: " + lastIndex);
		} else {
			firstIdx_view.setText("");
			lastIdx_view.setText("");
		}
	}
	
	
	/** 
	 * Set flow count view based on frame index 
	 * 
	 * Deprecated: I no longer use frame index to track playback progress
	 * because it fails to seek precisely when video speed is variable
	 */
//	public void setFlowCountView(int frameNumber) {
//		String frameIndex = Integer.toString(frameNumber);
//		int flowCount = -1;
//
//		if (preferences.contains(frameIndex)) {
//			flowCount = preferences.getInt(frameIndex, 0);
//			this.flowcount_view.setText(Integer.toString(flowCount));
//		}
//	
//		Log.i("flow", "flowCount[" + frameNumber + "]=" + flowCount);
//
//	}
	
	/**
	 * Set flow count view based on time stamp
	 * Updated on 2016.1.11
	 * 
	 * Input time stamp rather than frame index and read from SharedPreferences to get flow counts.
	 * Flow count per frame is calculated in the recording stage.
	 * Letting flow count display in the video player is just for testing only.
	 */
	public void setFlowCountView(int pts) {
		String videoTimeStamp = Integer.toString(pts);
		int flowCount = -1;

		if (preferences.contains(videoTimeStamp)) {
			flowCount = preferences.getInt(videoTimeStamp, 0);
			this.flowcount_view.setText(Integer.toString(flowCount));
		}
	}
	
	
	/**
	 * SurfaceHolder.callback. 
	 * This is used when using a SurfaceView
	 */
//	@Override
//    public void surfaceCreated(SurfaceHolder holder) {
//        Log.d(TAG, "surfaceCreated");
//    }
//
//    @Override
//    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//        Log.d(TAG, "surfaceChanged fmt=" + format + " size=" + width + "x" + height);
//    }
//
//    @Override
//    public void surfaceDestroyed(SurfaceHolder holder) {
//        Log.d(TAG, "Surface destroyed");
//    }

    
    public void startVideo() {
    	if (selectedFile != null) {		
    		/* used in SurfaceView */
//        	Surface surface = mSurfaceView.getHolder().getSurface();
    		
    		if (mHandler == null) {
        		mHandler = new MainHandler(this);
        	}
    		
    		initPlayer();
    		
        	
    		videoPlayer.startPlaying();//start the video decoding thread here
        	
    	} else {
    		Log.d(TAG, "failed to open file in: " + fileDirectory);
    	}
		
	}
    
    public void stopVideo() {
		if (videoPlayer != null) {
			videoPlayer.stopPlaying();//delete the thread
			videoPlayer = null;
			Log.i(TAG, "videoPlayer.stopPlaying()");
		}
		if (controller != null) {
			controller = null;	
		}
		if (mHandler != null) {
			mHandler = null;
		}
    }
    
    public void initPlayer() {
    	changeFPS(selectedFPS);
    			
		Log.i(TAG, " getting surfaceTexture...");
		surfaceTexture = mTextureView.getSurfaceTexture();

		Log.i(TAG, "initializing surface...");
		surface = new Surface(surfaceTexture);
		
	  	if (videoPlayer == null) {
	  		Log.i(TAG, "videoPlayer == null, initializing videoPlayer...");
	  		//initialize all the decoding stuff here.
	  		videoPlayer = new VideoPlayer(selectedFile, surface, controller, mHandler);
	    }  
	  	
	  	videoPlayer.setLoop(false);//set true to keep looping.
	  	
	  	adjustAspectRatio(videoPlayer.getWidth(), videoPlayer.getHeight());
    }
    
    public void changeFPS(int fps) {
    	if (controller == null) {
    		Log.i(TAG, "controller == null, initializing controller...");
    		controller = new MySpeedController(mHandler);
    	}
    	controller.setPlaybackRate(fps);
    }
    
    /**
     * disable UI when stop playing, enable when start playing.
     * @param enable: true to enable, false to disable
     * Added on 1/14/2016
     */
    public void updateUI(boolean enable) {
    	seekBar.setEnabled(enable);
    	mTextureView.setEnabled(enable);
    }
    
    
    public int getFirstIndex() {
    	return firstIndex;
    }
    
    public int getLastIndex() {
    	return lastIndex;
    }
    
    public boolean isAutoMode() {
    	return autoMode;
    }
    
    /**
     * Listener for setting the selected video file.
     */
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        Spinner spinner = (Spinner) parent;
        int mSelectedVideo = spinner.getSelectedItemPosition();

        Log.d(TAG, "onItemSelected: " + fileDirectory + mSelectedVideo + " '" + mVideoFiles[mSelectedVideo] + "'");
        String selectedFileName = fileDirectory + mVideoFiles[mSelectedVideo];
        selectedFile = new File(selectedFileName);
    }

    @Override public void onNothingSelected(AdapterView<?> parent) {}
    


    /* SurfaceTextureListener interface to implement */
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture arg0, int arg1,
			int arg2) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onSurfaceTextureAvailable");
	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onSurfaceTextureDestroyed");
		return false;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture arg0, int arg1,
			int arg2) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onSurfaceTextureDestroyed");
		
	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture arg0) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onSurfaceTextureUpdated");	
		
	}

	/**
     * Set the TextureView transform to preserve the aspect ratio of the video.
     */
    private void adjustAspectRatio(int videoWidth, int videoHeight) {
        int viewWidth = mTextureView.getWidth();
        int viewHeight = mTextureView.getHeight();
        double aspectRatio = (double) videoHeight / videoWidth;

        int newWidth, newHeight;
        if (viewHeight > (int) (viewWidth * aspectRatio)) {
            // limited by narrow width; restrict height
            newWidth = viewWidth;
            newHeight = (int) (viewWidth * aspectRatio);
        } else {
            // limited by short height; restrict width
            newWidth = (int) (viewHeight / aspectRatio);
            newHeight = viewHeight;
        }
        int xoff = (viewWidth - newWidth) / 2;
        int yoff = (viewHeight - newHeight) / 2;
        Log.v(TAG, "video=" + videoWidth + "x" + videoHeight +
                " view=" + viewWidth + "x" + viewHeight +
                " newView=" + newWidth + "x" + newHeight +
                " off=" + xoff + "," + yoff);

        Matrix txform = new Matrix();
        mTextureView.getTransform(txform);
        txform.setScale((float)newWidth / viewWidth, (float)newHeight / viewHeight);
        /* just for fun, this demonstrates the flexibility of using TextureView */
//        txform.postRotate(10); 
        txform.postTranslate(xoff, yoff);
        mTextureView.setTransform(txform);
    }

    public class SeekBarListener implements SeekBar.OnSeekBarChangeListener {
    	boolean isTrackingTouch = false;
    	long preProgress;
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			// TODO Auto-generated method stub
			Log.i("seekBar", "onProgressChanged: " + seekBar.getProgress());
			if (fromUser) {			
				videoPlayer.seekTo(progress, preProgress);
				Log.i("seekBar", "user moving bar");
			} else {//from the video playback itself
				Log.i("seekBar", "bar auto moving");
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			Log.i("seekBar", "onStartTrackingTouch: " + seekBar.getProgress());
			isTrackingTouch = true;
			
			preProgress = seekBar.getProgress();
		
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			Log.i("seekBar", "onStopTrackingTouch: " + seekBar.getProgress());
			isTrackingTouch = false;			
		}
		
		public boolean isTouch() {
			return isTrackingTouch;
		}
    	
    }
	
}


