package com.huawu.fivesmart.audio.apm;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.huawu.fivesmart.audio.apm.util.TXHAudioEncoderParam;
import com.huawu.fivesmart.audio.apm.util.TXIAudioEncoderListener;
import com.huawu.fivesmart.audio.apm.util.TestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class EchoActivity extends AppCompatActivity
{

    private Apm mApm;
    private AudioMediaCodecEncoder mAudioEncoder;
    private int j =0;
    private TestUtils mTestUtils;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_echo);

        mTestUtils = new TestUtils();
        intApm();
        //doAECM();
        //doAGC();
          doNS();
    }

    private void intApm()
    {
        mAudioEncoder = new AudioMediaCodecEncoder();
        startAudioAACEncoder();

        mApm = new Apm(false,false,false,false,false,false,false);

//        mApm.aecEnable(true);
//        mApm.aecSetSuppressionLevel(Apm.AEC_SuppressionLevel.HighSuppression);

//        mApm.vadEnable(true);
//        mApm.vadSetLikelihood(Apm.VAD_Likelihood.ModerateLikelihood);
//        mApm.aecmSetSuppressionLevel(Apm.AECM_RoutingMode.Speakerphone);
//        mApm.aecmEnable(true);

        //mApm.highPassfilterEnable(true);

        //-------- NS ---------
        mApm.nsEnable(true);
        mApm.nsSetLevel(Apm.NS_Level.Moderate);

        //-------- AGC ---------
//        mApm.agcEnableLimiter(true);
//        mApm.agcEnable(true);
//        mApm.agcSetMode(Apm.AGC_Mode.FixedDigital);
//        //mApm.agcSetAnalogLevelLimits(0,255);
//        mApm.agcSetCompressionGainDb(90); //[0,90]
//        mApm.agcSetTargetLevelDbfs(0);  //[0,31]
    }

    private void doNS()
    {
        try {

            FileInputStream fin = new FileInputStream(new File(
                    Environment.getExternalStorageDirectory().getPath()
                            + "/vtmp/12.wav"));

            long audioFileSize = fin.getChannel().size();
            int i = 0;

            final int cacheSize = 960;
            byte[] pcmInputCache = new byte[cacheSize];

            // core procession
            for (/* empty */; fin.read(pcmInputCache, 0, pcmInputCache.length) != -1; /* empty */) {
                // convert bytes[] to shorts[], and make it into little endian.
                short[] aecTmpIn = new short[cacheSize / 2];
                ByteBuffer.wrap(pcmInputCache).order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().get(aecTmpIn);

                i++;
                float progress = (float) cacheSize*i/audioFileSize;

                long time = System.currentTimeMillis();
                //Log.d("wangshuo","aecprogress = "+progress);

                mApm.ProcessStream(aecTmpIn,0);

                byte[] aecBuf = new byte[cacheSize];
                ByteBuffer.wrap(aecBuf).order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().put(aecTmpIn);

                mTestUtils.dumpWAVFile(aecBuf,48000,1);
                //mAudioEncoder.pushAudioFrameSync(aecBuf, getPTSUs(), false);
                Log.d("wangshuo","doNS aecBuf = "+aecBuf.length+" time = "+(System.currentTimeMillis()-time));
            }
            Toast.makeText(EchoActivity.this,"doNS 完成",Toast.LENGTH_SHORT).show();

            fin.close();
            mApm.destroy();
            mTestUtils.releaseWav();
        } catch (Exception e) {
            Log.e("wangshuo","e ="+e.getMessage());
            e.printStackTrace();
        }
    }

    public void doAECM() {
        try {

            FileInputStream fin = new FileInputStream(new File(
                    Environment.getExternalStorageDirectory().getPath()
                            + "/vtmp/a.pcm"));
            FileInputStream fin2 = new FileInputStream(new File(
                    Environment.getExternalStorageDirectory().getPath()
                            + "/vtmp/B.pcm"));

            FileOutputStream fout = new FileOutputStream(new File(
                    Environment.getExternalStorageDirectory().getPath()
                            + "/vtmp/srcOutoutOUT.pcm"));

            long audioFileSize = fin.getChannel().size();
            int i = 0;

            final int cacheSize = 960;
            byte[] pcmInputCache = new byte[cacheSize];
            byte[] pcmInputCache2 = new byte[cacheSize];

            // core procession
            for (/* empty */; fin.read(pcmInputCache, 0, pcmInputCache.length) != -1; /* empty */) {
                // convert bytes[] to shorts[], and make it into little endian.
                short[] aecTmpIn = new short[cacheSize / 2];
                ByteBuffer.wrap(pcmInputCache).order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().get(aecTmpIn);

                i++;
                float progress = (float) cacheSize*i/audioFileSize;

                long time = System.currentTimeMillis();
                Log.e("wangshuo","aecprogress = "+progress);


                mApm.setStreamDelayMs(150);

                if(fin2.read(pcmInputCache2, 0, pcmInputCache2.length) != -1){
                    short[] aecTmpOut = new short[cacheSize / 2];
                    ByteBuffer.wrap(pcmInputCache2).order(ByteOrder.LITTLE_ENDIAN)
                            .asShortBuffer().get(aecTmpOut);
                    mApm.ProcessReverseStream(aecTmpOut,0);
                }

                mApm.ProcessStream(aecTmpIn,0);



                //Log.e("wangshuo","aecTmpIn222 = "+aecTmpIn.length);
                // output
                byte[] aecBuf = new byte[cacheSize];
                ByteBuffer.wrap(aecBuf).order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().put(aecTmpIn);

                fout.write(aecBuf);
                mAudioEncoder.pushAudioFrameSync(aecBuf, getPTSUs(), false);
                Log.e("wangshuo","aecBuf = "+aecBuf.length+" time = "+(System.currentTimeMillis()-time));
            }
            Toast.makeText(EchoActivity.this,"完成",Toast.LENGTH_SHORT).show();
            fout.close();
            fin.close();
            mApm.destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void doAGC() {
        try {

            FileInputStream fin = new FileInputStream(new File(
                    Environment.getExternalStorageDirectory().getPath()
                            + "/vtmp/a1.wav"));

            long audioFileSize = fin.getChannel().size();
            int i = 0;

            final int cacheSize = 960;
            byte[] pcmInputCache = new byte[cacheSize];

            // core procession
            for (/* empty */; fin.read(pcmInputCache, 0, pcmInputCache.length) != -1; /* empty */) {
                // convert bytes[] to shorts[], and make it into little endian.
                short[] aecTmpIn = new short[cacheSize / 2];
                ByteBuffer.wrap(pcmInputCache).order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().get(aecTmpIn);

                i++;
                float progress = (float) cacheSize*i/audioFileSize;

                long time = System.currentTimeMillis();
                Log.d("wangshuo"," doAGC aecprogress = "+progress);

                mApm.ProcessStream(aecTmpIn,0);

                byte[] aecBuf = new byte[cacheSize];
                ByteBuffer.wrap(aecBuf).order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer().put(aecTmpIn);

                mTestUtils.dumpWAVFile(aecBuf,48000,1);
                //mAudioEncoder.pushAudioFrameSync(aecBuf, getPTSUs(), false);
                Log.d("wangshuo"," doAGC aecBuf = "+aecBuf.length+" time = "+(System.currentTimeMillis()-time));
            }
            Toast.makeText(EchoActivity.this," doAGC 完成",Toast.LENGTH_SHORT).show();

            fin.close();
            mApm.destroy();
            mTestUtils.releaseWav();
        } catch (Exception e) {
            Log.e("wangshuo","e ="+e.getMessage());
            e.printStackTrace();
        }
    }


    private long prevOutputPTSUs = 0;
    /**
     * get next encoding presentationTimeUs
     * @return
     */
    int i  = 0;
    protected long getPTSUs() {
//        long result = System.nanoTime() / 1000L;
//        // presentationTimeUs should be monotonic
//        // otherwise muxer fail to write
//        if (result < prevOutputPTSUs)
//            result = (prevOutputPTSUs - result) + result;

        long result = i * (480 * 1000*1000) / 48000;
        i++;
        return result;
    }

    private void startAudioAACEncoder()
    {
        mAudioEncoder.setListener(new TXIAudioEncoderListener() {
            @Override
            public void onEncodeAAC(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                if (byteBuffer == null || bufferInfo == null) {
                    return;
                }
                TestUtils.dumpAACFile(byteBuffer,bufferInfo,48000,1);
            }

            @Override
            public void onEncodeFormat(MediaFormat format) {

            }

            @Override
            public void onEncodeComplete() {
            }
        });

        TXHAudioEncoderParam param = new TXHAudioEncoderParam();
        param.channelCount = 1;
        param.sampleRate = 48000;
        param.audioBitrate = 200 * 1024;
        mAudioEncoder.start(param);
    }
}
