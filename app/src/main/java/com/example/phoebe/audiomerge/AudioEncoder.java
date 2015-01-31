package com.example.phoebe.audiomerge;

import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

/**
 * Created by Phoebe on 2/1/15.
 */
public class AudioEncoder {

    private static final String TAG = "AudioEncoder";
    private static final long DEFAULT_TIMEOUT_US = 50000;
    private MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private String mOutputFilename;
    private MediaCodec mEncoder;
    private boolean stopped = false;
    private RandomAccessFile mOutputFile;

    private int mSampleRate;

    public AudioEncoder(String outputFilename,int sampleRate) {
        this.mOutputFilename = outputFilename;
        this.mSampleRate = sampleRate;

        String mimeType = "audio/mp4a-latm";
        try {
            mOutputFile = new RandomAccessFile(outputFilename, "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }


        try {
            mEncoder = MediaCodec.createEncoderByType(mimeType);
            MediaFormat format = new MediaFormat();
            format.setString(MediaFormat.KEY_MIME, mimeType);
            format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 64 * 1000);//AAC-HE 64kbps
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectHE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }



    public void start() {
        mEncoder.start();
        mInputBuffers = mEncoder.getInputBuffers();
        mOutputBuffers = mEncoder.getOutputBuffers();
    }


    public synchronized void encode(byte[] frame, int offset, int size) throws IOException {
        if (stopped) {
            Log.e("encode end", "blocked");
            return;
        }
        Log.e("encode", "size = " + size);
        boolean sawInputEOS = size < 0;
        boolean sawOutputEOS = false;
        try {
            stopped |= sawInputEOS;
            if (stopped) {
                size = 0;
            } else {
                int inputBufIndex = mEncoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufIndex >= 0) {
                    mInputBuffers[inputBufIndex].clear();
                    mInputBuffers[inputBufIndex].put(frame);
                    mInputBuffers[inputBufIndex].rewind();
                    mEncoder.queueInputBuffer(
                            inputBufIndex,
                            offset,  // offset
                            size,  // size
                            0,
                            stopped ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                }
            }
            if (!stopped) {
                sawOutputEOS = dequeueOutput(false);
            } else {
                while (!sawOutputEOS) {
                    sawOutputEOS |= dequeueOutput(true);
                }

                mEncoder.stop();
                mEncoder.release();
            }

        } finally {
            if(sawOutputEOS)
            mOutputFile.close();
        }
    }

    /**
     * @return sawOutputEOS
     */
    private boolean dequeueOutput(boolean dropTimeOut) throws IOException {
        boolean sawOutputEOS = false;
        int result = mEncoder.dequeueOutputBuffer(mBufferInfo, DEFAULT_TIMEOUT_US);
        Log.e("encode end output", "" + result + " presentationTimeUs = " + mBufferInfo.presentationTimeUs);
        if (result >= 0) {
            int outputBufIndex = result;
            byte[] buffer = new byte[mBufferInfo.size];
            mOutputBuffers[outputBufIndex].rewind();
            mOutputBuffers[outputBufIndex].get(buffer, 0, mBufferInfo.size);
            Log.e("encode end flag", String.format("%h", mBufferInfo.flags));
            if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                sawOutputEOS = true;
            } else {
                mOutputFile.write(buffer);
            }
            mEncoder.releaseOutputBuffer(outputBufIndex,
                    false);  // render
        } else if (result == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            mOutputBuffers = mEncoder.getOutputBuffers();
        } else if (dropTimeOut && result == MediaCodec.INFO_TRY_AGAIN_LATER) {
            sawOutputEOS = true;
        }
        return sawOutputEOS;
    }

}
