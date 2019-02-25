package com.huawu.fivesmart.audio.apm.util;

import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;


public class BufferUtils
{
    private static final String TAG = "BufferUtils";

    /**
     * 从ByteBuffer中获取一个short数组
     *
     * @param buffer
     * @param length
     * @return
     */
    public static short[] getBuffer(ByteBuffer buffer, int length) {
        if (buffer == null) return null;

        int shortsLength = length / 2;

        short[] shorts = new short[shortsLength];

        buffer.order(ByteOrder.nativeOrder()).asShortBuffer().get(shorts);//注意buffer在底层存储是使用小端存储，所以获取的时候需要注意

        return shorts;
    }

    /**
     * 讲一个short数组 包装到ByteBuffer中去
     *
     * @param shorts
     * @return
     */
    public static ByteBuffer wrapBuffer(short[] shorts) {


        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(shorts.length * 2);

        byteBuffer.order(ByteOrder.nativeOrder()).asShortBuffer().put(shorts);

        byteBuffer.position(0);


        return byteBuffer;
    }

    /**
     * 尝试使用一个已经存在的ByteBuffer 进行包装short
     * <p>
     * 若不足够保存，那么则创建一个新的
     *
     * @param buffer
     * @param shorts
     * @return
     */
    public static ByteBuffer tryReUseBuffer(ByteBuffer buffer, short[] shorts) {
        int bufferLength = shorts.length * 2;
        if (buffer != null && buffer.capacity() >= bufferLength) {
            buffer.position(0);
            buffer.order(ByteOrder.nativeOrder()).asShortBuffer().put(shorts);
            buffer.limit(bufferLength);
            return buffer;
        } else {
            return wrapBuffer(shorts);//不足够存储， 那么重新创建
        }
    }

    //默认设置BGM和Video的声音都是1
    private static volatile float mBGMVolume = 1f;
    private static volatile float mVideoVolume = 1f;
    private static final short MIN_VALUE = -0x8000;   //-32768
    private static final short MAX_VALUE = 0x7FFF;    //32767

    public static void setBGMVolume(float bgmValue) {
        mBGMVolume = bgmValue;
    }

    public static void setVideoVolume(float videoVolume) {
        mVideoVolume = videoVolume;
    }

    public static byte[] mix(byte[] srcByte, byte[] bgmByte) {
        if(srcByte == null && bgmByte != null){
            Log.e("wangshuo","bgmByte---------");
            return  bgmByte;
        }
        if(srcByte != null && bgmByte == null){
            Log.e("wangshuo","srcByte---------");
            return  srcByte;
        }
        if(srcByte == null && bgmByte == null){
            Log.e("wangshuo","null---------");
            return  null;
        }

        Log.e("wangshuo","srcByte = "+srcByte.length);
        ByteBuffer byteBuffer = ByteBuffer.wrap(srcByte);
        short[] processDatashorts =  getBuffer(byteBuffer, srcByte.length);

        Log.e("wangshuo","bgmByte = "+bgmByte.length);
        ByteBuffer byteBuffer2 = ByteBuffer.wrap(bgmByte);
        short[] dataSysshorts =  getBuffer(byteBuffer2, bgmByte.length);

        short[] processResult = mix(dataSysshorts,processDatashorts);
        Log.e("wangshuo","processResult mix= "+processResult.length);

        ByteBuffer byteBufferResult = wrapBuffer(processResult);
        byte[] bytes = new byte[byteBufferResult.remaining()];
        byteBufferResult.get(bytes, 0, bytes.length);

        Log.e("wangshuo","final bytes = "+bytes.length);
        byteBufferResult.clear();
        byteBuffer.clear();
        byteBuffer2.clear();

        return bytes;
    }


    public static short[] mix(short[] srcShorts, short[] bgmShorts) {
        short[] mixedShorts = srcShorts;//这里直接对原数组进行操作，不新建数组节省开销
        for (int i = 0; i < srcShorts.length; i++) {
            int mixed = (int) (srcShorts[i] * mVideoVolume + bgmShorts[i] * mBGMVolume);
            short mixedShort = 0;//当混音之后的值超过最大或最小的阙值是会进行修正
            if (mixed > MAX_VALUE) {
                mixedShort = MAX_VALUE;
            } else if (mixed < MIN_VALUE) {
                mixedShort = MIN_VALUE;
            } else {
                mixedShort = (short) mixed;
            }
            mixedShorts[i] = mixedShort;
        }
        return mixedShorts;
    }
}
