package com.example.smartVideoPlayer;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

public class AudioDecoder {
	private static final String TAG = "AudioDecoder";
	private static final int TIMEOUT_USEC = 10000;
	MediaCodec mAudioDecoder;
	MediaExtractor mExtractor;
	MediaFormat mMediaFormat;
	String mSource;
	AudioTrack mAudioTrack;
	BufferInfo mBufferInfo;
	ByteBuffer[] mInputBuffers;
	ByteBuffer[] mOutputBuffers;
	long startMs;
	
	public AudioDecoder(String source) {
		mSource = source;
		setFormat();
		initAudioDecoder();
		
	}
	
	public void start() {
		decode();
	}
	
	private void setFormat() {
		mExtractor = new MediaExtractor();
		try {
			mExtractor.setDataSource(mSource);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i = 0; i < mExtractor.getTrackCount(); i++) {
			mMediaFormat = mExtractor.getTrackFormat(i);
			String mime = mMediaFormat.getString(MediaFormat.KEY_MIME);

			if (mime.startsWith("audio/")) {
				mExtractor.selectTrack(i);
				int audioBufSize = AudioTrack.getMinBufferSize(mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
	                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
				mAudioTrack = new AudioTrack(
					AudioManager.STREAM_MUSIC,
					mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
					mMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
					AudioFormat.ENCODING_PCM_16BIT,
					audioBufSize,
					AudioTrack.MODE_STREAM
				);
				break;
			}
		}
	}
	private void initAudioDecoder() {
		String mime = mMediaFormat.getString(MediaFormat.KEY_MIME);
		mAudioDecoder = MediaCodec.createDecoderByType(mime);
		mAudioDecoder.configure(mMediaFormat, null, null, 0);
		mAudioTrack.play();
		
		mAudioDecoder.start();
		mBufferInfo = new BufferInfo();
		
	}
	
	public void decode() {
		mInputBuffers = mAudioDecoder.getInputBuffers();
		mOutputBuffers = mAudioDecoder.getOutputBuffers();
		boolean isDecoding = true;
		boolean isEOS = false;
		
		startMs = System.currentTimeMillis();
		while(isDecoding) {
			int inIndex = mAudioDecoder.dequeueInputBuffer(TIMEOUT_USEC);
			if (inIndex >= 0) {
				ByteBuffer buffer = mInputBuffers[inIndex];
				if (!isEOS) {
					int size = mExtractor.readSampleData(buffer, 0);
					if (size < 0) {
						//end of stream
						mAudioDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
						isEOS = true;
					} else {
						mAudioDecoder.queueInputBuffer(inIndex, 0, size, mExtractor.getSampleTime(), 0);
						
						if (!isEOS)
							nextSample();
					}
				}
			}

			processDequeueBuffer();
			
			//end of stream
			if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {			
				break;
			}
		}
		mAudioDecoder.stop();
		mAudioDecoder.release();
		mExtractor.release();
		mAudioTrack.release();
	}
	
	private void nextSample() {
		//speed control
		while (mExtractor.getSampleTime() / 1000 > System.currentTimeMillis() - startMs) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		mExtractor.advance();
	}
	private void processDequeueBuffer() {
		int outIndex = mAudioDecoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
		switch (outIndex) {
		case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
			Log.d(TAG, "Audio INFO_OUTPUT_BUFFERS_CHANGED");
			mOutputBuffers = mAudioDecoder.getOutputBuffers();
			break;
		case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
			Log.d(TAG, "Audio new format " + mAudioDecoder.getOutputFormat());
			mAudioTrack.setPlaybackRate(mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE));
			break;
		case MediaCodec.INFO_TRY_AGAIN_LATER:
			Log.d(TAG, "Audio dequeueOutputBuffer timed out!");
			break;
		default:
			ByteBuffer tmpBuffer = mOutputBuffers[outIndex];
			final byte[] chunk = new byte[mBufferInfo.size];
			tmpBuffer.get(chunk); 
			tmpBuffer.clear();
			
			if (chunk.length > 0) {
				mAudioTrack.write(chunk, 0, chunk.length);
			}
			mAudioDecoder.releaseOutputBuffer(outIndex, false /* render */);
			break;
		}
	}
}
