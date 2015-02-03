package com.example.phoebe.audiomerge;

import android.content.Context;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Created by Phoebe on 2/1/15.
 */
public class AudioEncoderTest {
    private static final String TAG = "EncoderTest";
    private static final boolean VERBOSE = false;
    //    private static final int kNumInputBytes = 256 * 1024;
    private static final long kTimeoutUs = 10000;
    private OutputStream mFileOutputStream;
    public void testAACEncoders() {
        LinkedList<MediaFormat> formats = new LinkedList<MediaFormat>();
        final int kAACProfiles[] = {
                2 /* OMX_AUDIO_AACObjectLC */,
                5 /* OMX_AUDIO_AACObjectHE */,
                39 /* OMX_AUDIO_AACObjectELD */
        };
        final int kSampleRates[] = { 8000, 11025, 22050, 44100, 48000 };
        final int kBitRates[] = { 64000, 128000 };
        for (int k = 0; k < kAACProfiles.length; ++k) {
            for (int i = 0; i < kSampleRates.length; ++i) {
                if (kAACProfiles[k] == 5 && kSampleRates[i] < 22050) {
                    // Is this right? HE does not support sample rates < 22050Hz?
                    continue;
                }
                for (int j = 0; j < kBitRates.length; ++j) {
                    for (int ch = 1; ch <= 2; ++ch) {
                        MediaFormat format  = new MediaFormat();
                        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
                        format.setInteger(
                                MediaFormat.KEY_AAC_PROFILE, kAACProfiles[k]);
                        format.setInteger(
                                MediaFormat.KEY_SAMPLE_RATE, kSampleRates[i]);
                        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, ch);
                        format.setInteger(MediaFormat.KEY_BIT_RATE, kBitRates[j]);
                        formats.push(format);
                    }
                }
            }
        }
        //       testEncoder("audio/mp4a-latm", formats);
    }
    public int queueInputBuffer(
            MediaCodec codec, ByteBuffer[] inputBuffers, int index) {
        ByteBuffer buffer = inputBuffers[index];
        buffer.clear();
        int size = buffer.limit();
        byte[] zeroes = new byte[size];
        buffer.put(zeroes);
        codec.queueInputBuffer(index, 0 /* offset */, size, 0 /* timeUs */, 0);
        return size;
    }
    private void dequeueOutputBuffer(
            MediaCodec codec, ByteBuffer[] outputBuffers,
            int index, MediaCodec.BufferInfo info) {
        codec.releaseOutputBuffer(index, false /* render */);
    }


    public void testEncoder(byte[] inputData, String mime, MediaFormat format) {
        try {
            mFileOutputStream = new FileOutputStream(Environment.getExternalStorageDirectory() + "/jj.aac");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        MediaCodec codec = null;
        try {
            codec = MediaCodec.createEncoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            codec.configure(
                    format,
                    null /* surface */,
                    null /* crypto */,
                    MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IllegalStateException e) {
            Log.e(TAG, "codec '" + mime + "' failed configuration.");
        }

        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        codec.start();
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();
        int numBytesSubmitted = 0;
        boolean doneSubmittingInput = false;
        int numBytesDequeued = 0;
        int presentationTimeUS = 0;
        while (true) {
            int index;
            if (!doneSubmittingInput) {
                index = codec.dequeueInputBuffer(kTimeoutUs /* timeoutUs */);
                if (index != MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (numBytesSubmitted >= inputData.length) {
                        codec.queueInputBuffer(
                                index,
                                0 /* offset */,
                                0 /* size */,
                                presentationTimeUS /* timeUs */,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        if (VERBOSE) {
                            Log.d(TAG, "queued input EOS.");
                        }
                        doneSubmittingInput = true;
                    } else {


                        ByteBuffer buffer = codecInputBuffers[index];
                        buffer.clear();
                        int size = Math.min(buffer.limit(), inputData.length - numBytesSubmitted);
                        buffer.put(inputData, numBytesSubmitted, size);
                        codec.queueInputBuffer(index, 0 /* offset */, size, presentationTimeUS /* timeUs */, 0);

                        numBytesSubmitted += size;
                        presentationTimeUS = 500000 * numBytesSubmitted / sampleRate;
                        if (VERBOSE) {
                            Log.d(TAG, "queued " + size + " bytes of input data.");
                        }
                    }
                }

            }
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            index = codec.dequeueOutputBuffer(info, kTimeoutUs /* timeoutUs */);
            if (index >= 0) {
                int outBitsSize   = info.size;
                int outPacketSize = outBitsSize + 7;    // 7 is ADTS size
                ByteBuffer outBuf = codecOutputBuffers[index];

                outBuf.position(info.offset);
                outBuf.limit(info.offset + outBitsSize);
                try {
                    byte[] data = new byte[outPacketSize];  //space for ADTS header included
                    addADTStoPacket(data, outPacketSize);
                    outBuf.get(data, 7, outBitsSize);
                    outBuf.position(info.offset);
                    mFileOutputStream.write(data, 0, outPacketSize);  //open FileOutputStream beforehand
                } catch (IOException e) {
                    Log.e(TAG, "failed writing bitstream data to file");
                    e.printStackTrace();
                }

                numBytesDequeued += info.size;

                outBuf.clear();
                codec.releaseOutputBuffer(index, false /* render */);
                Log.d(TAG, "  dequeued " + outBitsSize + " bytes of output data.");
                Log.d(TAG, "  wrote " + outPacketSize + " bytes into output file.");
            }
            else if (index == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            }
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                if (VERBOSE) {
                    Log.d(TAG, "dequeued output EOS.");
                }
                break;
            }

            else if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();
            }
        }
        if (VERBOSE) {
            Log.d(TAG, "queued a total of " + numBytesSubmitted + "bytes, "
                    + "dequeued " + numBytesDequeued + " bytes.");
        }
        int channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        int inBitrate = sampleRate * channelCount * 16;  // bit/sec
        int outBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        float desiredRatio = (float)outBitrate / (float)inBitrate;
        float actualRatio = (float)numBytesDequeued / (float)numBytesSubmitted;
        if (actualRatio < 0.9 * desiredRatio || actualRatio > 1.1 * desiredRatio) {
            Log.w(TAG, "desiredRatio = " + desiredRatio
                    + ", actualRatio = " + actualRatio);
        }
        codec.release();
        codec = null;
        try {
            mFileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Add ADTS header at the beginning of each and every AAC packet.
     *  This is needed as MediaCodec encoder generates a packet of raw
     *  AAC data.
     *
     *  Note the packetLen must count in the ADTS header itself.
     **/
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        //39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 1;  //CPE

        // fill in ADTS data
        packet[0] = (byte)0xFF;
        packet[1] = (byte)0xF9;
        packet[2] = (byte)(((profile-1)<<6) + (freqIdx<<2) +(chanCfg>>2));
        packet[3] = (byte)(((chanCfg&3)<<6) + (packetLen>>11));
        packet[4] = (byte)((packetLen&0x7FF) >> 3);
        packet[5] = (byte)(((packetLen&7)<<5) + 0x1F);
        packet[6] = (byte)0xFC;
    }

}