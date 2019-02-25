package com.huawu.fivesmart.audio.apm.util;

import android.media.MediaCodec;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;


public class TestUtils {
//    private static final String BASE_PATH = Environment.getExternalStorageDirectory().getPath();
    private static final String BASE_PATH = "/sdcard/vtmp/";
    private static final String PATH_H264 = BASE_PATH + File.separator + "src.h264";
    private static final String PATH_AAC = BASE_PATH + File.separator + "src.aac";
    private static final String PATH_PCM = BASE_PATH + File.separator + "src.pcm";

    public static FileOutputStream dumpH264OS = null;
    public static FileOutputStream mAACFos = null;
    public static FileOutputStream mPCMFos = null;

    public static void dumpH264File(byte[] buffer) {
        try {
            if (dumpH264OS == null) {
                dumpH264OS = new FileOutputStream(new File(PATH_H264), false);
            }
            dumpH264OS.write(buffer);
            dumpH264OS.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void dumpAACFile(ByteBuffer encodedData, MediaCodec.BufferInfo bufferInfo, int sampleRate, int channels) {
        byte[] bytes = new byte[bufferInfo.size];
        encodedData.position(0);
        encodedData.get(bytes);
        encodedData.position(0);
        try {
            byte[] header = getADTSHeader(sampleRate, channels, bufferInfo.size);

            if (mAACFos == null)
                mAACFos = new FileOutputStream(new File(PATH_AAC));

            mAACFos.write(header);
            mAACFos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void dumpPCMFile(ByteBuffer buffer, MediaCodec.BufferInfo info) {
        byte[] bytes = new byte[info.size];
        buffer.position(0);
        buffer.get(bytes);
        buffer.position(0);
        try {
            if (mPCMFos == null)
                mPCMFos = new FileOutputStream(new File(PATH_PCM));
            mPCMFos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void dumpPCMFile(ByteBuffer buffer, int size) {
        byte[] bytes = new byte[size];
        buffer.position(0);
        buffer.get(bytes);
        buffer.position(0);
        try {
            if (mPCMFos == null)
                mPCMFos = new FileOutputStream(new File(PATH_PCM));
            mPCMFos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取AAC的ADTS头
     *
     * @param inSampleRate 输入采样率
     * @param channelCount 声道数
     * @param packetLen    AAC数据长度
     * @return ADTS头
     */
    private static byte[] getADTSHeader(int inSampleRate, int channelCount, int packetLen) {
        byte[] packet = new byte[7];
        int profile = 2;
        int freqIdx = 3;
        if (inSampleRate == 44100) {
            freqIdx = 4;
        } else if (inSampleRate == 48000) {
            freqIdx = 3;
        } else {
            throw new IllegalArgumentException("un support freq");
        }
        int chanCfg = channelCount;
        packetLen += 7;

        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
        return packet;
    }




}
