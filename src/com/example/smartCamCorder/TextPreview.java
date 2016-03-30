package com.example.smartCamCorder;

import com.example.utilities.Utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.text.Layout.Alignment;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

/**
 * Show text on camera preview.
 * Includes:
 * 1. debug text (frame#, flowcount, ...)
 * 2. time text
 * @author xuzebin
 *
 */
public class TextPreview extends View {
	private static final String TAG = "TextPreview";
	Paint paint = new Paint();
	TextPaint textPaint = new TextPaint();
	int result;
	int fps;
	float debugValue[];
	int pointCount;//number of moving optical flow pixels 
    int flowEnergy;
	int downsize_width;//size after downsize
	int downsize_height;
	
	float avg_max_bin_value;
	float max_bin_value;
	int max_bin_index;
	
	int frameNumber;
	
	TextPaint timePaint = new TextPaint();
	int timeStamp;
	String formattedTime;

	public TextPreview(Context context) {
		super(context);
		paint.setColor(Color.GREEN);
		paint.setStyle(Style.STROKE);
		paint.setTextSize(35f);
		paint.setStrokeWidth(10f);

		textPaint.setColor(Color.RED);
		textPaint.setTextSize(50f);
		
		timePaint.setColor(Color.GREEN);
		timePaint.setTextSize(60f);
		
		downsize_width = 0;
		downsize_height = 0;
		pointCount = 0;
		flowEnergy = 0;
		avg_max_bin_value = 0;
		frameNumber = 0;
		formattedTime = "00:00";
	}
	@Override
	protected void onDraw(Canvas canvas) {		
		//draw optical flow
//		for (int i = 0; i < pointCount; ++i) {
//			if (pointX[i] < 0 || pointY[i] < 0 || pointX[i] > 100 || pointY[i] > 100) continue;
//			canvas.drawLine((float)pointX[i], (float)pointY[i], (float)(pointX[i] + flowX[i]), (float)(pointY[i] + flowY[i]), paint);
////			System.out.println("pointx: " + pointX[i] + " flowx:" + flowX[i]);
////				canvas.drawLine(x, y, x+10, y+10, paint);
//		}
		
		/* motion results */
		String result;
		//64x64
//		if (pointCount >= 3000) result = "整体运动";
//		else if (pointCount > 800 && pointCount < 3000) result = "显著运动";
//		else result = "非显著运动/静止";
		
		//48x48
//		if (pointCount >= 1700) result = "整体运动";
//		else if (pointCount > 100 && pointCount < 1700) result = "显著运动";
//		else result = "非显著运动/静止";
		
		//36x36
//		if (pointCount >= 800) result = "整体运动";
//		else if (pointCount > 100 && pointCount < 800) result = "显著运动";
//		else result = "非显著运动/静止";
		

		
		/* display messages on screen */
		String msg = downsize_width + "x" + downsize_height + "\n";
		
		msg += "flowCount: " + pointCount + "\n"; //+
//					result + "\n" +
//					"fps: " + fps + " \n";
		msg += "flowEnergy: " + flowEnergy + "\n";
		msg += "avg_max_bin: " + avg_max_bin_value + "\n";
		msg += "max_bin[" + max_bin_index + "] = " + max_bin_value + "\n";
		msg += "frames: " + frameNumber + "\n";
		if ( debugValue != null) {
			msg += "time: " + debugValue[0] + "ms\n";
			
			//time mean
			msg += "time mean: " + debugValue[1] + "ms\n";
			
//			//time max, min
//			msg += "time max: " + debugValue[2] + "ms\n" + 
//					"time min: " + debugValue[3] + "ms\n";
			
			//time, time mean in java
//			msg += "time3: " + debugValue[8] + "ms\n";// + 
//					"time3 mean: " + debugValue[9] + "ms\n";
			
			msg += "time_delta: " + debugValue[2] + "ms\n" + 
					"encode_time: " + debugValue[3] + "ms\n" +
					"algorithm_time: " + debugValue[4] + "ms\n";
 			
			/* maximum norm of optical flow */
//			float of_norm = debugValue[9];
//			msg += "norm: " + of_norm + "\n";
//			if ((of_norm > 20 && pointCount > 600) || of_norm > 30) msg += "Movement\n";
//			else msg += "jitter\n";
			
			//max_bin_index, max_bin
			msg += "max_bin[" + debugValue[9] + "] = " + debugValue[10] + "\n";
			
			//flow_count
			msg += "flow_count: " + debugValue[11] + "\n";
			//small_flow_count
			msg += "small_flow_count: " + debugValue[7] + "\n";
			
			//avg
			msg += "average max_bin: " + debugValue[12] + "\n";
			
		}
					
		
		/* Draw debug values */
		StaticLayout layout = new StaticLayout(msg, textPaint, 650, Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
		canvas.save();  
		canvas.translate(20, 900);
		layout.draw(canvas);  
		canvas.restore();


		//get formatted time text from time stamp
		formattedTime = Utils.formatTime(timeStamp);
		
		/* Draw time */
		StaticLayout timeLayout = new StaticLayout(formattedTime, timePaint, 650, Alignment.ALIGN_NORMAL, 1.0F, 0.0F, true);
		canvas.translate(20, 20);
		timeLayout.draw(canvas);
		
		canvas.restore();
		
		super.onDraw(canvas);
		
	}

	public void setDebugValue(float[] debugValue) {
		this.debugValue = debugValue;
	}
	public void setResult(int result) {
		this.result = result;
	}
	public void setFPS(int fps) {
		this.fps = fps;
	}

	public void setPointCount(int pointCount) {
		this.pointCount = pointCount;
	}
	
	public void setFlowEnergy(int flowEnergy) {
		this.flowEnergy = flowEnergy;
	}
	
	public void setDownsize(int width, int height) {
		downsize_width = width;
		downsize_height = height;
	}
	
	public void setAvgMaxBin(float avg_max_bin_value) {
		this.avg_max_bin_value = avg_max_bin_value;
	}
	
	public void setMaxBin(float max_bin_value) {
		this.max_bin_value = max_bin_value;
	}
	
	public void setMaxBinIndex(int index) {
		this.max_bin_index = index;
	}
	
	public void setFrameNumber(int frame) {
		frameNumber = frame;
	}
	
	public void reset() {
		setFrameNumber(0);
		setMaxBinIndex(0);
		setMaxBin(0);
		setAvgMaxBin(0);
		setDownsize(0, 0);
		setPointCount(0);
		setFlowEnergy(0);
	}
	
	public void setTimeStamp(int timeStamp_) {
		timeStamp = timeStamp_;
	}
}
