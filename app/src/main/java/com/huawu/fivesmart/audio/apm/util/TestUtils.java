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
    private static final String PATH_WAV = BASE_PATH + File.separator + "src.wav";

    public static FileOutputStream dumpH264OS = null;
    public static FileOutputStream mAACFos = null;
    public static FileOutputStream mPCMFos = null;
    private FileOutputStream mWavFos = null;

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

    public void dumpWAVFile(byte[] buffer,int mSampleRate,int mChannels) {
        try {
            if (mWavFos == null){
                mWavFos = new FileOutputStream(new File(PATH_WAV));
                writeWaveFileHeader(mWavFos,1024*1000*1000,1024*1000*1000+36,mSampleRate,mChannels,16);
            }
            mWavFos.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void releaseWav() {
        if(mWavFos != null)
        {
            try
            {
                mWavFos.close();
                mWavFos = null;
            } catch (IOException e)
            {
                e.printStackTrace();
            }
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

    /**
     * 输出WAV文件
     * @param out WAV输出文件流
     * @param totalAudioLen 整个音频PCM数据大小
     * @param totalDataLen 整个数据大小
     * @param sampleRate 采样率
     * @param channels 声道数
     * @param byteRate 采样字节byte率
     * @throws IOException
     */
    public static void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                           long totalDataLen, int sampleRate, int channels, long byteRate) throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);//数据大小
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';//WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        //FMT Chunk
        header[12] = 'f'; // 'fmt '
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';//过渡字节
        //数据大小
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        //编码方式 10H为PCM编码格式
        header[20] = 1; // format = 1
        header[21] = 0;
        //通道数
        header[22] = (byte) channels;
        header[23] = 0;
        //采样率，每个通道的播放速度
        header[24] = (byte) (sampleRate & 0xff);
        header[25] = (byte) ((sampleRate >> 8) & 0xff);
        header[26] = (byte) ((sampleRate >> 16) & 0xff);
        header[27] = (byte) ((sampleRate >> 24) & 0xff);
        //音频数据传送速率,采样率*通道数*采样深度/8
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
        header[32] = (byte) (channels * 16 / 8);
        header[33] = 0;
        //每个样本的数据位数
        header[34] = 16;
        header[35] = 0;
        //Data chunk
        header[36] = 'd';//data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }
}
