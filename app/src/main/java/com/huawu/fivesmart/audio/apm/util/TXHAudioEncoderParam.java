package com.huawu.fivesmart.audio.apm.util;

import java.io.Serializable;

/**
 * Created by liyuejiao on 2018/3/1.
 * 音频编码需要的参数
 */

public class TXHAudioEncoderParam implements Serializable {
    public int channelCount;
    public int sampleRate;
    public int audioBitrate;
}
