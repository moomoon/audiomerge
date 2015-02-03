package com.example.phoebe.audiomerge;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by Phoebe on 1/31/15.
 */
public class AudioDecoder {
    private final static long TIMEOUT_US = 5000L;
    private final static String TAG = "merger";
    private String mFilePath;
    private int mNumChannel;
    private int mSampleRate;
    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private final DecodedSample mResult;
    private boolean mInputEOS = false;
    private boolean mOutputEOS = false;


    private long mPresentationTimeUs = 0L;

    public AudioDecoder(String filePath) {
        this.mFilePath = filePath;
        this.mResult = new DecodedSample();
    }

    public DecodedSample getSample() {
        return mResult;
    }

    public void prepare() {
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(mFilePath);
            for (int i = 0; i < mExtractor.getTrackCount(); i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {
                    mNumChannel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    mDecoder = MediaCodec.createDecoderByType(mime);
                    mDecoder.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
                    Log.e(TAG, "path = " + mFilePath + " sampleRate = " + mSampleRate + " mNumchannel = " + mNumChannel);
                    mExtractor.selectTrack(i);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long getSamplePresentationTimeUs() {
        return mPresentationTimeUs;
    }

    public boolean pollSample() {
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        if (!mInputEOS) {
            int inputBufIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
            if (inputBufIndex >= 0) {
                ByteBuffer dstBuf = mInputBuffers[inputBufIndex];
                int sampleSize =
                        mExtractor.readSampleData(dstBuf, 0 /* offset */);
                if (sampleSize < 0) {
                    Log.e(TAG, "saw input EOS.");
                    mInputEOS = true;
                    sampleSize = 0;
                } else {
                    mPresentationTimeUs = mExtractor.getSampleTime();
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < sampleSize / 2 * 2; i += 2) {
                        sb.append(String.format("[%d]%h", i, dstBuf.getShort(i)));
                    }

                    Log.e("readSample", sb.toString());
                }
                mDecoder.queueInputBuffer(
                        inputBufIndex,
                        0 /* offset */,
                        sampleSize,
                        mPresentationTimeUs,
                        mInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                if (!mInputEOS) {
                    mExtractor.advance();
                }
            }

        }
        int res = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);
        if (res >= 0) {
            //Log.d(TAG, "got frame, size " + info.size + "/" + info.presentationTimeUs);

            int outputBufIndex = res;
            ByteBuffer buf = mOutputBuffers[outputBufIndex];
            buf.position(0);
            mResult.set(buf, info.size / 2);
            mDecoder.releaseOutputBuffer(outputBufIndex, false /* render */);
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.e(TAG, "saw output EOS.");
                mOutputEOS = true;
            }
        } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            mOutputBuffers = mDecoder.getOutputBuffers();
            Log.e(TAG, "output buffers have changed.");
        } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat oformat = mDecoder.getOutputFormat();
            Log.e(TAG, "output format has changed to " + oformat);
        } else {
            Log.e(TAG, "dequeueOutputBuffer returned " + res);
        }
        return !mOutputEOS;
    }

    public int getChannelCount() {
        return mNumChannel;
    }

    public int getSampleRate() {
        return mSampleRate;
    }


    public void start() {
        mDecoder.start();
        mInputBuffers = mDecoder.getInputBuffers();
        mOutputBuffers = mDecoder.getOutputBuffers();
    }

    public void release() {
        mExtractor.release();
        mExtractor = null;
        mDecoder.stop();
        mDecoder.release();
        mDecoder = null;
    }

    public static class DecodedSample {
        private short[] data;
        private int size;
        private int offset;

        public DecodedSample() {
            data = new short[0];
            size = 0;
            offset = 0;
        }

        private void set(ByteBuffer buf, int size) {
            if (size > this.size) {
                data = Arrays.copyOf(data, size);
            }
            this.size = size;
            for (int i = 0; i < size; i++) {
                data[i] = buf.getShort(i * 2);
            }
            //           StringBuilder sb = new StringBuilder();
            //           for(int i = 0; i < size; i ++){
            //               sb.append(String.format("[%d]%04h", i % 2, data[i]));
            //           }
            //           Log.e(TAG + " set", sb.toString());
        }

        public void get(DecodedSample other) {
            if (other.size < this.size) {
                other.data = Arrays.copyOf(this.data, this.size);
            } else {
                System.arraycopy(this.data, 0, other.data, 0, this.size);
            }
            other.size = this.size;
        }

        public boolean valid(int id) {
            return id < size;
        }

        public short get(int id) {
          //  if (id % 2 == 0) {
                //    return (short) ((data[id] & 0xff00) | ((data[id + 1] ) & 0xff));
                return data[id];
          //  }
           // return (short) (((data[id] << 8) & 0xff00) | ((data[id] >> 8) & 0xff));
            //return (short) ((data[id - 1] << 8) & 0xff00 | ((data[id] >> 8)& 0xff));
        }
    }

    @Override
    public String toString() {
        return TAG + " path = " + mFilePath + " channels = " + mNumChannel + " sampleRate = " + mSampleRate;
    }
}