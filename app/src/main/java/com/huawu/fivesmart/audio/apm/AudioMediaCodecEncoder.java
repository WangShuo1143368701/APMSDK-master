package com.huawu.fivesmart.audio.apm;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import com.huawu.fivesmart.audio.apm.util.TXCThread;
import com.huawu.fivesmart.audio.apm.util.TXHAudioEncoderParam;
import com.huawu.fivesmart.audio.apm.util.TXIAudioEncoderListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.TreeSet;

public class AudioMediaCodecEncoder
{
    private static final String TAG = AudioMediaCodecEncoder.class.getSimpleName();
    private static final int MAX_INPUT_SIZE = 10000;
    private static final long TIMEOUT_USEC = 10000;

    private TXCThread mEncThread;
    private TXIAudioEncoderListener mListener;

    private boolean mInit;
    private int mPushIdx;
    private int mPopIdx;
    private long mLatey;
    private boolean isEnd;

    private TreeSet<Long> mAudioPtsSet;

    private MediaCodec mMediaCodec;
    private Long mNewPts;
    private final Object mLock = new Object();

    public AudioMediaCodecEncoder() {
        mEncThread = new TXCThread("HWAudioEncoder");
    }

    public void setListener(TXIAudioEncoderListener listener) {
        mListener = listener;
    }

    //openGl Thread Call
    public void start(final TXHAudioEncoderParam param) {
        synchronized (this) {
            mEncThread.runSync(new Runnable() {
                @Override
                public void run() {
                    if (mInit) {
                        return;
                    }
                    startInner(param);
                }
            });
        }
    }

    //Main/Encoder Thread Call
    public void stop() {
        synchronized (this) {
            mEncThread.runSync(new Runnable() {
                @Override
                public void run() {
                    if (mInit) {
                        stopInner();
                        mEncThread.getHandler().removeCallbacksAndMessages(null);
                    }
                }
            });
        }
    }

    private void startInner(TXHAudioEncoderParam param) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        MediaCodecInfo audioCodecInfo = selectCodec(MediaFormat.MIMETYPE_AUDIO_AAC);
        MediaFormat format = createAudioFormat(param);

        if (audioCodecInfo == null || format == null) return;

        String codecName = audioCodecInfo.getName();
        try {
            mMediaCodec = MediaCodec.createByCodecName(codecName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();

        mInit = true;
        mEncThread.runAsyncDelay(decodeOutput, 1);
        mAudioPtsSet = new TreeSet<>();
        mNewPts = 0L;
    }

    private void stopInner() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
        }
        mInit = false;
    }

    public void pushAudioFrameSync(byte[] chuck, long ptsUs) {
        pushAudioFrameSync(chuck,ptsUs,false);
    }

    public void pushAudioFrameSync(byte[] chuck, long ptsUs,boolean isEnd) {
        this.isEnd = isEnd;
        mPushIdx++;
//        TXCLog.i(TAG, "mPushIdx:" + mPushIdx + ", frame time = " + frame.getSampleTime() + ",flag:" + frame.getFlags() + ", Queue:" + mAudioQueue.size() + ",length:" + frame.getLength());

        synchronized (mLock) {
            if (mAudioPtsSet != null) {
                mAudioPtsSet.add(ptsUs);
//                TLog.d(TAG, "mAudioPtsSet.add = "+ ptsUs);
            }
        }
        while (!Thread.interrupted()) {
            if (!mInit)
                return;

            int inputIndex = findFreeFrame();
            if (inputIndex < 0) {
                continue;
            }

            encodeAudioFrame(inputIndex, chuck, ptsUs);
            break;
        }

        if (isEnd) {
            while (!Thread.interrupted()) {
                if (!mInit)
                    return;

                int inputIndex = findFreeFrame();
                if (inputIndex < 0) {
                    continue;
                }

                mMediaCodec.queueInputBuffer(inputIndex, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);

                break;
            }
        }
    }

    private int findFreeFrame() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return -1;
        }
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
        if (inputBufferIndex >= 0) {
            ByteBuffer byteBuffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                byteBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
            } else {
                ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                byteBuffer = inputBuffers[inputBufferIndex];
            }
            byteBuffer.clear();
            return inputBufferIndex;
        }
        return inputBufferIndex;
    }

    private void encodeAudioFrame(int inputIndex, byte[] chuck, long ptsUs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        ByteBuffer buffer;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            buffer = mMediaCodec.getInputBuffer(inputIndex);
        } else {
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            buffer = inputBuffers[inputIndex];
        }

//        if (frame.isEndFrame()) {
//            mMediaCodec.queueInputBuffer(index, 0, 0, frame.getSampleTime(), MediaCodec.BUFFER_FLAG_END_OF_STREAM);
//            return;
//        }
        buffer.rewind();

        if (chuck.length < buffer.remaining()) {
            buffer.put(chuck);
            mMediaCodec.queueInputBuffer(inputIndex, 0, chuck.length, ptsUs, 0);
        } else {
            String errMsg = "input size is larger than buffer capacity. should increate buffer capacity by setting MediaFormat.KEY_MAX_INPUT_SIZE while configure. mime = ";
            throw new IllegalArgumentException(errMsg);
        }
    }

    private Runnable decodeOutput = new Runnable() {
        @Override
        public void run() {
            if (!mInit)
                return;
            onDecodeOutput();
        }
    };

    private void onDecodeOutput() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return;
        }
        if (mMediaCodec == null) {

            mEncThread.runAsyncDelay(decodeOutput, 10);
            return;
        }
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        int encoderStatus = mMediaCodec.dequeueOutputBuffer(info, TIMEOUT_USEC);
        if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            mEncThread.runAsyncDelay(decodeOutput, 10);
            mLatey++;
            if(isEnd && mLatey > 100){
                mListener.onEncodeAAC(null, null);
                mLatey = 0;
                isEnd = false;
            }
            return;
        } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
            encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            MediaFormat mediaFormat = mMediaCodec.getOutputFormat();
            if (mListener != null) {
                mListener.onEncodeFormat(mediaFormat);
            }
        } else if (encoderStatus < 0) {
        } else {
            ByteBuffer byteBuffer;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                byteBuffer = mMediaCodec.getOutputBuffer(encoderStatus);
            } else {
                byteBuffer = encoderOutputBuffers[encoderStatus];
            }
            if (byteBuffer == null) {
                throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null.mime:");
            }
            byte[] aacData = new byte[info.size];
            byteBuffer.position(info.offset);
            byteBuffer.limit(info.offset + info.size);
            byteBuffer.get(aacData, 0, info.size);

            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                info.size = 0;
            }
            if (mListener != null && info.size != 0) {
                mPopIdx++;
//                TXCLog.d(TAG, "mPopIdx:" + mPopIdx + ", info flags = " + info.flags + ", pts:" + info.presentationTimeUs);
                info.presentationTimeUs = calculateCurrentFramePTS();

                MediaCodec.BufferInfo copy = new MediaCodec.BufferInfo();
                ByteBuffer buffer = ByteBuffer.wrap(aacData);
                copy.set(info.offset, aacData.length, info.presentationTimeUs, info.flags);
                mListener.onEncodeAAC(buffer, info);
            }

            mMediaCodec.releaseOutputBuffer(encoderStatus, false);

            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                if (mListener != null) {
                    mListener.onEncodeComplete();
                }
                return;
            }
        }

        mEncThread.runAsyncDelay(decodeOutput, 10);
    }

    protected long calculateCurrentFramePTS() {
        synchronized (mLock) {
            if (!mAudioPtsSet.isEmpty()) {
                mNewPts = mAudioPtsSet.pollFirst();
//                TLog.d(TAG, "calculateCurrentFramePTS = "+ mNewPts);
                return mNewPts;
            }
        }
        //FIXBUG:小米6 pop出去的比push进来的帧数要多，所以封装时时间戳不能顺序递增
        mNewPts = mNewPts + 100;
        return mNewPts;
    }

    private MediaFormat createAudioFormat(TXHAudioEncoderParam param) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, param.sampleRate, param.channelCount);
            format.setInteger(MediaFormat.KEY_BIT_RATE, param.audioBitrate);
            format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE);
            format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, MAX_INPUT_SIZE);
            return format;
        }
        return null;
    }

    private static MediaCodecInfo selectCodec(String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            int numCodecs = MediaCodecList.getCodecCount();
            for (int i = 0; i < numCodecs; i++) {
                MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

                if (!codecInfo.isEncoder()) {
                    continue;
                }

                String[] types = codecInfo.getSupportedTypes();

                for (String type : types) {
                    if (type.equalsIgnoreCase(mimeType)) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

}
