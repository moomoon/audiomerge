package com.example.phoebe.audiomerge;

import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Phoebe on 1/31/15.
 */
public class AudioMerger {


    private final String TAG;
    //   private List<AudioSource> mAudioSources;
    private List<DecoderWrapper> mDecoders;
    private int mSampleRate;
    private int mDurationNS;
    private int mNumChannel;
    private byte[] mDecoded;
    private int mDecodedIndex;
    private int mDataSize;
    private long mDecodedTimeNS;
    private int mSampleTimeNS;


    //   private static class AudioSource {
    //       private String filePath;
    //       private int delayMS;
    //   }

    public AudioMerger(String tag) {
        this.TAG = tag;
        mDecoders = new ArrayList<>();
    }

    public void addSource(String filePath, int delayMS) {
//        AudioSource as = new AudioSource();
//        as.filePath = filePath;
//        as.delayMS = delayMS;
//        if (null == mAudioSources) {
//            mAudioSources = new ArrayList<>();
//        }
        //       mAudioSources.add(as);
        DecoderWrapper decoder = new DecoderWrapper(filePath, delayMS * 1000000);
        mDecoders.add(decoder);
    }


    public void prepare(int durationMS) {
        int minSampleRate = Integer.MAX_VALUE;
        for (DecoderWrapper decoder : mDecoders) {
            decoder.prepare();
            minSampleRate = Math.min(minSampleRate, decoder.getDecoderCore().getSampleRate());
        }
        this.mDurationNS = durationMS * 1000000;
        this.mSampleRate = minSampleRate;
        this.mNumChannel = 1;
        this.mDataSize = mSampleRate * mNumChannel * durationMS / 500;
        mDecoded = new byte[mDataSize];
        mDecodedTimeNS = 0;
        mSampleTimeNS = 1000000000 / mSampleRate;
        mDecodedIndex = 0;
    }

    public void merge() {
        for (DecoderWrapper decoder : mDecoders) {
            decoder.start();
        }
        //FIXME implemented only for two
        DecoderWrapper decoder0 = mDecoders.get(0);
        DecoderWrapper decoder1 = mDecoders.get(1);
        Log.e(TAG, "start dataSize = " + mDataSize + " sampleRate = " + mSampleRate + " sampleTime = " + mSampleTimeNS);

        while (mDecodedIndex < mDataSize) {
//            Log.e(TAG, "time = " + mDecodedTimeNS);
            short mixed = mix(decoder0.getValue(mDecodedTimeNS), decoder1.getValue(mDecodedTimeNS));
            if (mDecodedIndex % 1000 == 0) {
  //              Log.e(TAG + mDecodedTimeNS, "value = " + mDecoded[mDecodedIndex]);
            }

            mDecoded[mDecodedIndex ++] = (byte)(mixed & 0xFF);
            mDecoded[mDecodedIndex ++] = (byte)((mixed >>8) & 0xFF);
            mDecodedTimeNS += mSampleTimeNS;
        }
        for (DecoderWrapper decoder : mDecoders) {
            decoder.release();
        }

        MediaFormat format  = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(
                MediaFormat.KEY_AAC_PROFILE, 2);
        format.setInteger(
                MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
        new AudioEncoderTest().testEncoder(mDecoded,"audio/mp4a-latm", format);
        Log.e(TAG, "end");
    }


    private short mix(short sample0, short sample1) {
        float sampleF0 = sample0 / 32768.0f;
        float sampleF1 = sample1 / 32768.0f;

        float mixed = sampleF0 + sampleF1;
        // reduce the volume a bit:
        mixed *= 0.8;
        // hard clipping
        if (mixed > 1.0f) mixed = 1.0f;
        if (mixed < -1.0f) mixed = -1.0f;
        return (short) (mixed * 32768.0f);
    }

    private class DecoderWrapper {
        private AudioDecoder decoderCore;
        private AudioDecoder.DecodedSample decodedSample;
        private int sampleTimeNS;
        private int numChannel;
        private long frameTimeNS;
        private long delayTimeNS;
        private short currentIndex;
        private boolean eos = false;


        private DecoderWrapper(String filePath, long delayTimeNS) {
            decoderCore = new AudioDecoder(filePath);
            decodedSample = new AudioDecoder.DecodedSample();
            this.delayTimeNS = delayTimeNS;
            this.frameTimeNS = delayTimeNS;
        }

        private void prepare() {
            decoderCore.prepare();
            this.sampleTimeNS = 1000000000 / decoderCore.getSampleRate();
            this.frameTimeNS = sampleTimeNS;
            this.numChannel = decoderCore.getChannelCount();
        }

        private void start() {
            decoderCore.start();
        }

        private AudioDecoder getDecoderCore() {
            return decoderCore;
        }


        private short getValue(long timeNS) {
            if (eos || timeNS < delayTimeNS) {
                return 0;
            }
            if (timeNS <= frameTimeNS) {
                while (!eos && !decodedSample.valid(currentIndex)) {
  //                  Log.e(TAG, "call poll 0");
                    pollSample();
                    currentIndex = 0;
                }
                if (eos) {
//                    return 0;
                }
                return decodedSample.get(currentIndex);
            }
            while (!eos && frameTimeNS < timeNS) {
                frameTimeNS += sampleTimeNS;
                currentIndex+= numChannel;
                if (!decodedSample.valid(currentIndex)) {
 //                   Log.e(TAG, "frameNS = " + frameTimeNS + " timeNS = " + timeNS + " sampleTimeNs = " + sampleTimeNS);
                    pollSample();
                    currentIndex = 0;
                }
            }
            if (eos) {
                return 0;
            }
            return decodedSample.get(currentIndex);
        }

        private synchronized void pollSample() {
            eos = !decoderCore.pollSample();
            if (eos) {
                Log.e(TAG, "eos");
                release();
            } else {
                decoderCore.getSample().get(decodedSample);
            }
        }

        private synchronized void release() {
            if (!eos) {
                eos = true;
                decoderCore.release();
                decoderCore = null;
                decodedSample = null;
            }
        }
    }


}
