package com.example.smartNative;

/**
 * Java Interfaces for algorithms in native layer.
 * @author xuzebin
 *
 */
public class SmartNative {
  
    /**
     * calculate optical flow
     * @param data:  frame data
     * @param width: width of frame
     * @param height: height of frame
     * @param frame_index: current frame index
     * @param time_stamp: current frame time stamp
     * @return
     */
    public static native int calOpticalFlow(byte[] data, int width, int height, int frame_index, long time_stamp);
    
    public static native int[] getResultIndexes();
    public static native float[] getDebugValue();
    public static native int[] getDownSize();
    
    static {
        System.loadLibrary("smart_kit_jni");
    }
}
