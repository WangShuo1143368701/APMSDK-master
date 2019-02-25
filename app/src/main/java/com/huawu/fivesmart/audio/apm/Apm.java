package com.huawu.fivesmart.audio.apm;


import android.util.Log;

public class Apm
{
    static {
        System.loadLibrary("webrtc_audio");
    }

    public enum AEC_SuppressionLevel{
        LowSuppression,
        ModerateSuppression,
        HighSuppression
    }

    public enum AECM_RoutingMode {
        QuietEarpieceOrHeadset,
        Earpiece,
        LoudEarpiece,
        Speakerphone,
        LoudSpeakerphone
    }

    public enum AGC_Mode {
        AdaptiveAnalog,
        AdaptiveDigital,
        FixedDigital
    }

    public enum NS_Level {
        Low,
        Moderate,
        High,
        VeryHigh
    }

    public enum VAD_Likelihood {
        VeryLowLikelihood,
        LowLikelihood,
        ModerateLikelihood,
        HighLikelihood
    }


    private boolean init = false;
    private long objData;
    private static final String TAG = "webrtc_apm";

    public Apm(boolean aecExtendFilter, boolean speechIntelligibilityEnhance, boolean delayAgnostic, boolean beamforming, boolean nextGenerationAec, boolean experimentalNs, boolean experimentalAgc){
        if(nativeCreateApmInstance(aecExtendFilter, speechIntelligibilityEnhance, delayAgnostic, beamforming, nextGenerationAec, experimentalNs, experimentalAgc)){
            init = true;
        }else {
            Log.e(TAG,"Apm failed create");
        }
    }

    public int aecSetSuppressionLevel(AEC_SuppressionLevel aec_suppressionLevel){
           return aecSetSuppressionLevel(aec_suppressionLevel.ordinal());
    }

    public int aecmSetSuppressionLevel(AECM_RoutingMode aecm_routingMode){
        return aecmSetSuppressionLevel(aecm_routingMode.ordinal());
    }

    public int nsSetLevel(NS_Level ns_level){
        return nsSetLevel(ns_level.ordinal());
    }

    public int agcSetMode(AGC_Mode agc_mode){
        return agcSetMode(agc_mode.ordinal());
    }

    public int vadSetLikelihood(VAD_Likelihood vad_likelihood){
        return vadSetLikelihood(vad_likelihood.ordinal());
    }

    public void destroy() {
        if(init){
            nativeFreeApmInstance();
            init = false;
        }
    }
    

    //------------------------------------  native  ---------------------------------

    public native boolean nativeCreateApmInstance(boolean aecExtendFilter, boolean speechIntelligibilityEnhance, boolean delayAgnostic, boolean beamforming, boolean nextGenerationAec, boolean experimentalNs, boolean experimentalAgc);

    public native void nativeFreeApmInstance();

    public native int highPassfilterEnable(boolean enable);

    //AEC
    public native int aecEnable(boolean enable);

    public native int aecSetSuppressionLevel(int level);  //[0, 1, 2]

    public native int aecClockDriftCompensationEnable(boolean enable);

    //AECM
    public native int aecmEnable(boolean enable);

    public native int aecmSetSuppressionLevel(int level);  //[0, 1, 2, 3, 4]

    //NS
    public native int nsEnable(boolean enable);

    public native int nsSetLevel(int level);  // [0, 1, 2, 3]

    //AGC
    public native int agcEnable(boolean enable);

    public native int agcSetTargetLevelDbfs(int level);   //[0,31]

    public native int agcSetCompressionGainDb(int db);  //[0,90]

    public native int agcEnableLimiter(boolean enable);

    public native int agcSetAnalogLevelLimits(int min,int max);  // limit to [0, 65535]

    public native int agcSetMode(int mode);     // [0, 1, 2]

    public native int agcSetStreamAnalogLevel(int level);

    public native int agcStreamAnalogLevel();

    //VAD
    public native int vadEnable(boolean enable);

    public native int vadSetLikelihood(int level);

    public native boolean vadStreamHasVoice();


    public native int ProcessStream(short[] date, int offset);

    public native int ProcessReverseStream(short[] date, int offset);

    public native int setStreamDelayMs(int delay);

    public native int setSampleRateAndChannels(int sampleRat,int channels);

}
