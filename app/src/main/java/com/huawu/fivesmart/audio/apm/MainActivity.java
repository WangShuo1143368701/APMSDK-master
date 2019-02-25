package com.huawu.fivesmart.audio.apm;

import android.app.Activity;
import android.content.Intent;
import android.media.*;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.huawu.fivesmart.audio.apm.util.BufferUtils;
import com.huawu.fivesmart.audio.apm.util.TXHAudioEncoderParam;
import com.huawu.fivesmart.audio.apm.util.TXIAudioEncoderListener;
import com.huawu.fivesmart.audio.apm.util.TestUtils;

import java.io.*;
import java.nio.ByteBuffer;

public class MainActivity extends Activity implements View.OnClickListener
{

    static {
        System.loadLibrary("webrtc_audio");
    }

    Button startRecordingButton, stopRecordingButton,mButtonEcho;//开始录音、停止录音
    File recordingFile;//储存AudioRecord录下来的文件
    boolean isRecording = false; //true表示正在录音
    AudioRecord audioRecord=null;
    File parent=null;//文件目录
    int bufferSize=0;//最小缓冲区大小
    int sampleRateInHz = 48000;//采样率
    int channelConfig = 1; //单声道
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT; //量化位数
    String TAG="AudioRecord";

    private AudioMediaCodecEncoder mAudioEncoder;
    private AudioDecoderThread audioDecoderThread;
    private Apm mApm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView tv_hello = (TextView) findViewById(R.id.tv_hello);
        tv_hello.setText(helloFromHWAPMSDK());

        initAudioEncoder();
        init();
        initListener();

        intApm();
    }

    private void intApm()
    {
       mApm = new Apm(false,false,false,false,false,false,true);

        //mApm.nsEnable(true);
        //mApm.nsSetLevel(Apm.NS_Level.VeryHigh);

        mApm.agcSetAnalogLevelLimits(0,255);
        mApm.agcEnableLimiter(true);
        mApm.agcEnable(true);
        mApm.agcSetCompressionGainDb(9);
        mApm.agcSetTargetLevelDbfs(6);
        mApm.agcSetMode(Apm.AGC_Mode.FixedDigital);
    }

    private void initAudioEncoder()
    {
        mAudioEncoder = new AudioMediaCodecEncoder();
        audioDecoderThread = new AudioDecoderThread();
    }

    //初始化
    public void init(){
        startRecordingButton = (Button)findViewById(R.id.btn_start_recoder);
        stopRecordingButton = (Button)findViewById(R.id.btn_stop_recoder);
        mButtonEcho = (Button)findViewById(R.id.btn_echo);

        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz,channelConfig, audioFormat);//计算最小缓冲区
        //MediaRecorder.AudioSource.VOICE_COMMUNICATION
        //MediaRecorder.AudioSource.MIC
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRateInHz,channelConfig, audioFormat, bufferSize);//创建AudioRecorder对象

        parent = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+ "/AudioRecordTest");
        if(!parent.exists())
            parent.mkdirs();//创建文件夹
    }

    //初始化监听器
    public void initListener(){
        startRecordingButton.setOnClickListener(this);
        stopRecordingButton.setOnClickListener(this);
        mButtonEcho.setOnClickListener(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            //开始录音
            case R.id.btn_start_recoder:
            {
                if(!isRecording){
                    Toast.makeText(this, "开始录制", Toast.LENGTH_SHORT).show();
                record();
                 }

                break;
            }

            //停止录音
            case R.id.btn_stop_recoder:
            {
                if(isRecording){
                    Toast.makeText(this, "停止录制", Toast.LENGTH_SHORT).show();
                    stopRecording();
                }
                break;
            }
            case R.id.btn_echo:
                Intent intent = new Intent(MainActivity.this,EchoActivity.class);
                startActivity(intent);
                break;
        }


    }

    //开始录音
    public void record() {
        isRecording = true;
        startAudioAACEncoder();
        new Thread(new Runnable() {
            @Override
            public void run() {
                isRecording = true;

                recordingFile = new File(parent,"audiotest.pcm");
                if(recordingFile.exists()){
                    recordingFile.delete();
                }

                try {
                    recordingFile.createNewFile();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG,"创建储存音频文件出错");
                }


                try {
                    final DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(recordingFile)));
                    final byte[] buffer = new byte[2048];
                    audioRecord.startRecording();//开始录音
                    int r = 0;
                    final byte[][] bgmData = {null};
                    final int[] out_analog_level = {200};

                    audioDecoderThread.startPlay(Environment.getExternalStorageDirectory().getPath() + "/2.mp4", new AudioDecoderThread.AudioDecoderData()
                    {
                        @Override
                        public void getAudioDecoderData(byte[] chunk, long ptsUs)
                        {
                            bgmData[0] =  chunk;
                            //Log.e("wangshuo","chunk = "+chunk.length);

//                        ByteBuffer byteBuffer = ByteBuffer.wrap(chunk);
//                        short[] byteBuffershorts =  BufferUtils.getBuffer(byteBuffer, chunk.length);
//                        Log.e("ProcessStream","ProcessStream = "+byteBuffershorts.length);
//                        //Apm.getInstance().agcSetStreamAnalogLevel(out_analog_level[0]);
//                        Apm.getInstance().ProcessStream(byteBuffershorts,0);
//                        //out_analog_level[0] = Apm.getInstance().agcStreamAnalogLevel();
//
//                        Log.e("ProcessStream","ProcessStream2 = "+byteBuffershorts.length);
//                        ByteBuffer byteBufferResult = BufferUtils.wrapBuffer(byteBuffershorts);
//                        byte[] bytes = new byte[byteBufferResult.remaining()];
//                        byteBufferResult.get(bytes, 0, bytes.length);
//                        byteBuffer.clear();
//                        byteBufferResult.clear();
//                            mAudioEncoder.pushAudioFrameSync(bytes, ptsUs, false);
//                            TestUtils.dumpPCMFile(byteBufferResult,chunk.length);
                        }
                    });

                    while (isRecording) {
                        final int bufferReadResult = audioRecord.read(buffer,0,buffer.length);
                        //byte[] mixBuffer = BufferUtils.mix(buffer,bgmData[0]);

                        ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
                        final short[] byteBuffershorts =  BufferUtils.getBuffer(byteBuffer, buffer.length);
                        if(bgmData[0] != null){
                            ByteBuffer bgmbyteBuffer = ByteBuffer.wrap(bgmData[0]);
                            final short[] bgmbyteBuffershorts =  BufferUtils.getBuffer(byteBuffer, bgmData[0].length);
                            Log.e("ProcessStream","ProcessStream = "+byteBuffershorts.length);

                            processStream(byteBuffershorts,bgmbyteBuffershorts, sampleRateInHz, mApm, new AudioApmData()
                            {
                                @Override
                                public void getAudioApmData(short[] chunk,short[] bgmChunk)
                                {
                                    ByteBuffer byteBufferResult = BufferUtils.wrapBuffer(chunk);
                                    byte[] bytes = new byte[byteBufferResult.remaining()];
                                    byteBufferResult.get(bytes, 0, bytes.length);
                                    byteBufferResult.clear();
                                    Log.e("wangshuo","getAudioApmData = "+bytes.length);
                                    mAudioEncoder.pushAudioFrameSync(bytes, getPTSUs(), false);
                                    TestUtils.dumpPCMFile(byteBufferResult,bytes.length);

                                }
                            });
                        }


//
                        for (int i = 0; i < bufferReadResult; i++) //写出未处理之前的PCM
                        {
                            dos.write(buffer[i]);
                        }
                        r++;
                    }
                    audioRecord.stop();//停止录音
                    dos.close();
                } catch (Throwable t) {
                    Log.e(TAG, "Recording Failed");
                }

            }
        }).start();

    }

    //停止录音
    public void stopRecording()
    {
        if (mAudioEncoder != null) {
            mAudioEncoder.stop();
        }
        if(audioDecoderThread != null){
            audioDecoderThread.stop();
        }
        isRecording = false;
    }

    private void startAudioAACEncoder()
    {
        mAudioEncoder.setListener(new TXIAudioEncoderListener() {
            @Override
            public void onEncodeAAC(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo) {
                if (byteBuffer == null || bufferInfo == null) {
                    return;
                }
                TestUtils.dumpAACFile(byteBuffer,bufferInfo,sampleRateInHz,channelConfig);
            }

            @Override
            public void onEncodeFormat(MediaFormat format) {

            }

            @Override
            public void onEncodeComplete() {
            }
        });

        TXHAudioEncoderParam param = new TXHAudioEncoderParam();
        param.channelCount = channelConfig;
        param.sampleRate = sampleRateInHz;
        param.audioBitrate = 200 * 1024;
        mAudioEncoder.start(param);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        stopRecording();
        mApm.nativeFreeApmInstance();
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


    private short[] mRightSrcData = null;
    private short[] mRightBGMData = null;
    /**
     * 合并两个数组
     *
     * @param leftData
     * @param srcData
     * @return
     */
    private short[] merge(short[] leftData, short[] srcData) {
        if (leftData == null || leftData.length == 0)
            return srcData;
        short[] merge = new short[leftData.length + srcData.length];
        System.arraycopy(leftData, 0, merge, 0, leftData.length);
        System.arraycopy(srcData, 0, merge, leftData.length, srcData.length);
        return merge;
    }

    /**
     * 获取剩余数组
     *
     * @return
     */
    private short[] getSRCRight(short[] mergeShort, int LOOP_COUNT, int sampleRate_10ms) {
        short[] data = new short[mergeShort.length - LOOP_COUNT*sampleRate_10ms];
        System.arraycopy(mergeShort, LOOP_COUNT*sampleRate_10ms, data, 0, mergeShort.length-LOOP_COUNT*sampleRate_10ms);
        return data;
    }

    private void processStream(short[] srcShort,short[] bgmShort , int sampleRate,Apm apm ,AudioApmData audioApmData){
        int sampleRate_10ms = sampleRate/100;
        short[] mergeShort = merge(mRightSrcData,srcShort);
        int LOOP_COUNT = mergeShort.length / sampleRate_10ms;
        mRightSrcData = null;

        short[] mergeBGMShort = merge(mRightBGMData,bgmShort);
        mRightBGMData = null;

        for(int i =0 ; i < LOOP_COUNT ; i++){
          short[] shorts = new short[sampleRate_10ms];
          System.arraycopy(mergeShort, i*sampleRate_10ms, shorts, 0, sampleRate_10ms);
          apm.ProcessStream(shorts,0);

          short[] bgmShorts = new short[sampleRate_10ms];
          System.arraycopy(mergeBGMShort, i*sampleRate_10ms, bgmShorts, 0, sampleRate_10ms);
          apm.ProcessReverseStream(bgmShorts,0);

          if(audioApmData != null){
              audioApmData.getAudioApmData(shorts,bgmShorts);
              Log.e("wangshuo","shorts = "+shorts.length +" bgmShorts = "+bgmShorts.length);
          }
        }
        mRightSrcData = getSRCRight(mergeShort,LOOP_COUNT,sampleRate_10ms);
        mRightBGMData = getSRCRight(mergeBGMShort,LOOP_COUNT,sampleRate_10ms);
    }



    public interface AudioApmData {
        void getAudioApmData(short[] chunk, short[] bgmChunk);
    }

    public interface AudioBGMData {
        void getAudioBGMData(short[] chunk);
    }



    //===================================== native ================================

    public native String helloFromHWAPMSDK();

}
