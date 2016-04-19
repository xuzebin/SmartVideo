package com.example.smartCamCorder;


import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaMuxer;
import android.util.Log;

/**
 * VideoEncoder includes the core components of a video encoder 
 * and a muxer to write into .mp4 file.
 * The encoder encode frames from preview callback.
 * Improved version of Grafika's.
 * @author xuzebin
 * 
 * Added:
 * 1. Time stamps adjustments.
 * 2. Encode every frame as the key frame.
 * 	(This is important for the video player to seek precisely)
 * 
 * Usage: 
 * 1. Initialize encoder with initEncoder().
 * 2. call encodeOneFrame() in onPreviewFrame().
 * 3. call stop() when recording is done.
 * 
 * TODO:
 * 1. Add audio encoder.
 * 2. Encode surface directly (more efficient).
 * 3. Adjust some parameters based on real-time frame rate to suit more cases.
 *
 */
public class VideoEncoder {
	private static final String TAG = "VideoEncoder";

	private static final int TIMEOUT_USEC = 10000;
	private static final int ONE_THOUSAND = 1000;
	private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
	
	/**
	 * The unit is SECOND. To ensure every frame
	 * is the key frame here the I_FRAME_INTERVAL is 0 second
	 */
	private static final int I_FRAME_INTERVAL = 0;
	private static final int FRAME_RATE = 120;
	private static final int COMPRESS_RATIO = 128;//256;
	private static final int COLOR_CHANNEL = 3;
	private static final int BITS_PER_BYTE = 8;
	private static int bit_rate; //This is determined by frames size, frame rate and compress ratio.
	private static int color_format;
	
	private int previewWidth;
	private int previewHeight;
	private String fileName;
	private MediaCodec encoder;
	private MediaMuxer muxer;
	private BufferInfo bufferInfo;
	private int trackIndex;
	private boolean muxerStarted;
	private byte[] frameData;
		
	//in millisecond (used in getRealTimeStamp())
	private int timeStamp;
	private long prevTimeStamp;

	@SuppressLint("NewApi")
	public VideoEncoder(String fileName_, int width, int height) {	
		previewWidth = width;
		previewHeight = height;
		fileName = fileName_;
		
		frameData = new byte[previewWidth * previewHeight * COLOR_CHANNEL / 2];
		timeStamp = -1;
		prevTimeStamp = 0;
	}
	
	/**
	 * Must be called and only needs to be called once before encodeOneFrame()
	 * @return true if success
	 */
	public boolean initEncoder() {
		bit_rate = previewWidth * previewHeight * COLOR_CHANNEL * BITS_PER_BYTE * FRAME_RATE / COMPRESS_RATIO;
		
		bufferInfo = new MediaCodec.BufferInfo();
		MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
		if (codecInfo == null) {
			Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
			return false;
		}

		color_format = selectColorFormat(codecInfo, MIME_TYPE);

		MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
						previewWidth, previewHeight);

		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bit_rate);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, color_format);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);

		encoder = MediaCodec.createByCodecName(codecInfo.getName());
		encoder.configure(mediaFormat, null, null,
				MediaCodec.CONFIGURE_FLAG_ENCODE);
		encoder.start();
		
		initMuxer();
		
		return true;
	}
	
	/* Used for Muxing the encoded video into an mp4 file. */
	private void initMuxer() {
		// Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
		try {
			muxer = new MediaMuxer(fileName,
					MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		} catch (IOException ioe) {
			throw new RuntimeException("MediaMuxer creation failed", ioe);
		}
		trackIndex = -1;
	    muxerStarted = false;
	}

	public int encodeOneFrame(byte[] input) {
		NV21toI420SemiPlanar(input, frameData, previewWidth, previewHeight);

		ByteBuffer[] inputBuffers = encoder.getInputBuffers();
		ByteBuffer[] outputBuffers = encoder.getOutputBuffers();
		int inputBufferId = encoder.dequeueInputBuffer(TIMEOUT_USEC);

		long currentTimeStamp = 0;//current time stamp starts from a non-zero value
		/* input buffers */
		if (inputBufferId >= 0) {
			ByteBuffer inputBuffer = inputBuffers[inputBufferId];
			inputBuffer.put(frameData);
			currentTimeStamp = System.nanoTime() / ONE_THOUSAND;
			encoder.queueInputBuffer(inputBufferId, 0,
					frameData.length, currentTimeStamp, 0);
			Log.i("pts", "currentTimeStamp=" + currentTimeStamp);
		} else {
			// either all in use, or we timed out during initial setup
			Log.d(TAG, "input buffer not available");
		}
		
		/* output buffers */
		int outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
		do {
			if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// Subsequent data will conform to new format.
				MediaFormat newFormat = encoder.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                trackIndex = muxer.addTrack(newFormat);
                muxer.start();
                muxerStarted = true;
			} else if (outputBufferId >= 0) {
				ByteBuffer outputBuffer = outputBuffers[outputBufferId];			
				if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferId +
                            " was null");
                }
				
				if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    bufferInfo.size = 0;
                }

				if (bufferInfo.size != 0) {
					if (!muxerStarted) {
						MediaFormat newFormat = encoder.getOutputFormat();
						trackIndex = muxer.addTrack(newFormat);
			            muxer.start();
			            muxerStarted = true;
					}
					// adjust the ByteBuffer values to match BufferInfo (not needed?)
//					outputBuffer.position(mBufferInfo.offset);
//					outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);

					muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);//write the encoded frame to file.
					Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer" 
					+ " bufferinfo.pts=" + bufferInfo.presentationTimeUs);
				}
				encoder.releaseOutputBuffer(outputBufferId, false);
				Log.i(TAG, "outputBufferId: " + outputBufferId);
			}
			outputBufferId = encoder.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
		} while(outputBufferId >= 0);
		
		int realTimeStamp = getRealTimeStamp(currentTimeStamp / ONE_THOUSAND);
		Log.i(TAG, "currentTimeStamp = " + (currentTimeStamp / ONE_THOUSAND) + "ms, writableTimeStamp: " + realTimeStamp + "ms");
		return realTimeStamp;//return a real time stamp that starts from 0
	}

	public void stop() {
		Log.i(TAG, "stop()");
		try {
			encoder.stop();
			encoder.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (muxer != null) {
            // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
            //       of frames submitted, and don't call stop() if we haven't written anything.
            muxer.stop();
            muxer.release();
            muxer = null;
        }
	}

	/**
	 * NV21 is a 4:2:0 YCbCr, For 1 NV21 pixel: YYYYYYYY VUVU I420YUVSemiPlanar
	 * is a 4:2:0 YUV, For a single I420 pixel: YYYYYYYY UVUV Apply NV21 to
	 * I420YUVSemiPlanar(NV12) Refer to https://wiki.videolan.org/YUV/
	 */
	private void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes,
			int width, int height) {
		System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
		for (int i = width * height; i < nv21bytes.length; i += 2) {
			i420bytes[i] = nv21bytes[i + 1];
			i420bytes[i + 1] = nv21bytes[i];
		}
	}

	/**
	 * Returns a color format that is supported by the codec and by this test
	 * code. If no match is found, this throws a test failure -- the set of
	 * formats known to the test should be expanded for new platforms.
	 */
	private static int selectColorFormat(MediaCodecInfo codecInfo,
			String mimeType) {
		MediaCodecInfo.CodecCapabilities capabilities = codecInfo
				.getCapabilitiesForType(mimeType);
		for (int i = 0; i < capabilities.colorFormats.length; i++) {
			int colorFormat = capabilities.colorFormats[i];
			if (isRecognizedFormat(colorFormat)) {
				return colorFormat;
			}
		}
		Log.e(TAG,
				"couldn't find a good color format for " + codecInfo.getName()
						+ " / " + mimeType);
		return 0; // not reached
	}

	/**
	 * Returns true if this is a color format that this test code understands
	 * (i.e. we know how to read and generate frames in this format).
	 */
	private static boolean isRecognizedFormat(int colorFormat) {
		switch (colorFormat) {
		// these are the formats we know how to handle for this test
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns the first codec capable of encoding the specified MIME type, or
	 * null if no match was found.
	 */
	private static MediaCodecInfo selectCodec(String mimeType) {
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
			if (!codecInfo.isEncoder()) {
				continue;
			}
			String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				if (types[j].equalsIgnoreCase(mimeType)) {
					return codecInfo;
				}
			}
		}
		return null;
	}

	/**
	 * Returns true if the specified color format is semi-planar YUV. Throws an
	 * exception if the color format is not recognized (e.g. not YUV).
	 */
	private static boolean isSemiPlanarYUV(int colorFormat) {
		switch (colorFormat) {
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
			return false;
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
			return true;
		default:
			throw new RuntimeException("unknown format " + colorFormat);
		}
	}
	
	/**
	 * Convert the current time stamp to a real time stamp that starts from 0
	 * @param currentTime current time stamp (do not start from 0) unit: millisecond
	 * @return real time stamp (start from 0)
	 */
	public int getRealTimeStamp(long currentTime) {
		if (timeStamp == -1) {//first frame
			timeStamp = 0;
			prevTimeStamp = currentTime;
		} else {
			timeStamp += currentTime - prevTimeStamp;
			prevTimeStamp = currentTime;
		}
		return timeStamp;
	}
}
