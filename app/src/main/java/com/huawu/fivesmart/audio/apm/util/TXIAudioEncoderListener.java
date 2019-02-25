package com.huawu.fivesmart.audio.apm.util;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * Created by liyuejiao on 2018/3/1.
 */

public interface TXIAudioEncoderListener {

    void onEncodeAAC(ByteBuffer byteBuffer, MediaCodec.BufferInfo bufferInfo);

    void onEncodeFormat(MediaFormat format);

    void onEncodeComplete();
}
