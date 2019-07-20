

#include <apm_processing.h>

static void set_ctx(JNIEnv *env, jobject thiz, void *ctx) {
    jclass cls = env->GetObjectClass(thiz);
    jfieldID fid = env->GetFieldID(cls, "objData", "J");
    env->SetLongField(thiz, fid, (jlong)ctx);
}

static void *get_ctx(JNIEnv *env, jobject thiz) {
    jclass cls = env->GetObjectClass(thiz);
    jfieldID fid = env->GetFieldID(cls, "objData", "J");
    return (void*)env->GetLongField(thiz, fid);
}

#ifdef __cplusplus
extern "C" {
#endif


    JNIEXPORT jint JNI_OnLoad(JavaVM *vm , void *reserved ) {
        return JNI_VERSION_1_6;
    }

JNIEXPORT jstring JNICALL Java_com_huawu_fivesmart_audio_apm_MainActivity_helloFromHWAPMSDK(JNIEnv *env, jobject /* this */)
{
    std::string hello = "Hello from webrtc!!!";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jboolean JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_nativeCreateApmInstance
        (JNIEnv *env, jobject thiz, jboolean aecExtendFilter, jboolean speechIntelligibilityEnhance, jboolean delayAgnostic, jboolean beamforming, jboolean nextGenerationAec, jboolean experimentalNs, jboolean experimentalAgc) {

        ApmProcessing* apm = new ApmProcessing;
        if (apm == nullptr)
            return JNI_FALSE;
        else {
            apm->Init(48000,1);
            set_ctx(env, thiz, apm);
            LOGV("created");
            return JNI_TRUE;
        }
    }


JNIEXPORT void JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_nativeFreeApmInstance
(JNIEnv *env, jobject thiz) {
    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
        apm->Release();
        delete apm;
        LOGV("destroyed");
    }


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_highPassfilterEnable
        (JNIEnv *env, jobject thiz, jboolean enable){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->high_pass_filter()->Enable(enable);
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_aecEnable
        (JNIEnv *env, jobject thiz, jboolean enable){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->echo_cancellation()->Enable(enable);
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_aecClockDriftCompensationEnable
(JNIEnv *env, jobject thiz, jboolean enable){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->echo_cancellation()->enable_drift_compensation(enable);

}

JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_aecSetSuppressionLevel
        (JNIEnv *env, jobject thiz, jint level){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    if ( level < EchoCancellation::kLowSuppression ){
        level = EchoCancellation::kLowSuppression;
    }else if(level > EchoCancellation::kHighSuppression){
        level = EchoCancellation::kHighSuppression;
    }
    return apm->_apm->echo_cancellation()->set_suppression_level((EchoCancellation::SuppressionLevel)level);
}



JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_aecmEnable
        (JNIEnv *env, jobject thiz, jboolean enable){
    LOGV("aecm_enable");
    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->echo_control_mobile()->Enable(enable);
}

JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_aecmSetSuppressionLevel
        (JNIEnv *env, jobject thiz, jint level){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    if(level < EchoControlMobile::kQuietEarpieceOrHeadset){
        level = EchoControlMobile::kQuietEarpieceOrHeadset;
    }else if(level > EchoControlMobile::kLoudSpeakerphone){
        level = EchoControlMobile::kLoudSpeakerphone;
    }
    return apm->_apm->echo_control_mobile()->set_routing_mode((EchoControlMobile::RoutingMode)level);

}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_nsSetLevel
(JNIEnv *env, jobject thiz, jint level){

    if(level < NoiseSuppression::kLow){
        level = NoiseSuppression::kLow;
    }else if(level > NoiseSuppression::kVeryHigh){
        level = NoiseSuppression::kVeryHigh;
    }


    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->noise_suppression()->set_level((NoiseSuppression::Level)level);
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_nsEnable
(JNIEnv *env, jobject thiz, jboolean enable){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->noise_suppression()->Enable(enable);
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_agcSetAnalogLevelLimits
(JNIEnv *env, jobject thiz, jint minimum, jint maximum){

    if(minimum < 0){
        minimum = 0;
    }else if(minimum > 65535){
        minimum = 65535;
    }

    if(maximum < 0){
        maximum = 0;
    }else if(maximum > 65535){
        maximum = 65535;
    }

    if(minimum > maximum){
        int temp = minimum;
        minimum = maximum;
        maximum = temp;
    }

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->gain_control()->set_analog_level_limits(minimum, maximum);
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_agcSetMode
(JNIEnv *env, jobject thiz, jint mode){

    if(mode < GainControl::Mode::kAdaptiveAnalog){
        mode = GainControl::Mode::kAdaptiveAnalog;
    }else if(mode > GainControl::Mode::kFixedDigital){
        mode = GainControl::Mode::kFixedDigital;
    }

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->gain_control()->set_mode((GainControl::Mode)mode);
}

JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_agcSetTargetLevelDbfs
  (JNIEnv *env, jobject thiz, jint level){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->gain_control()->set_target_level_dbfs(level);
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_agcSetCompressionGainDb
        (JNIEnv *env, jobject thiz, jint gain){
    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->gain_control()->set_compression_gain_db(gain);
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_agcEnableLimiter
        (JNIEnv *env, jobject thiz, jboolean enable){
    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->gain_control()->enable_limiter(enable);
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_agcEnable
(JNIEnv *env, jobject thiz, jboolean enable){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->gain_control()->Enable(enable);

}

JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_agcSetStreamAnalogLevel
(JNIEnv *env, jobject thiz, jint level){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->gain_control()->set_stream_analog_level(level);
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_agcStreamAnalogLevel
        (JNIEnv *env, jobject thiz){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->gain_control()->stream_analog_level();
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_vadEnable
(JNIEnv *env, jobject thiz, jboolean enable){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->voice_detection()->Enable(enable);

}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_vadSetLikelihood
        (JNIEnv *env, jobject thiz, jint likelihood){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    if(likelihood < VoiceDetection::kVeryLowLikelihood){
        likelihood = VoiceDetection::kVeryLowLikelihood;
    }else if(likelihood > VoiceDetection::kHighLikelihood){
        likelihood = VoiceDetection::kHighLikelihood;
    }


    return apm->_apm->voice_detection()->set_likelihood((VoiceDetection::Likelihood)likelihood);
}


JNIEXPORT jboolean JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_vadStreamHasVoice
        (JNIEnv *env, jobject thiz){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->voice_detection()->stream_has_voice();
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_ProcessStream
        (JNIEnv *env, jobject thiz, jshortArray nearEnd, jint offset){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    short *buffer = (short*)env->GetShortArrayElements(nearEnd, nullptr);
    int ret = apm->ProcessStream(buffer + offset);
    env->ReleaseShortArrayElements(nearEnd, buffer, 0);
    return ret;
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_ProcessReverseStream
        (JNIEnv *env, jobject thiz, jshortArray farEnd, jint offset){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    short *buffer = (short*)env->GetShortArrayElements(farEnd, nullptr);
    int ret = apm->ProcessReverseStream(buffer + offset);

    env->ReleaseShortArrayElements(farEnd, buffer, 0);
    return ret;
}


JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_setStreamDelayMs
        (JNIEnv *env, jobject thiz, jint delay){

    ApmProcessing *apm = (ApmProcessing*) get_ctx(env, thiz);
    return apm->_apm->set_stream_delay_ms(delay);
}

JNIEXPORT jint JNICALL Java_com_huawu_fivesmart_audio_apm_Apm_setSampleRateAndChannels
        (JNIEnv *env, jobject thiz, jint sampleRat, jint channels) {

    //ApmWrapper *apm = (ApmWrapper *) get_ctx(env, thiz);
    //apm->_apm->set_sample_rate_hz(sampleRat);
    //apm->_apm->set_num_channels(channels, channels);
    return 0;
}

#ifdef __cplusplus
}
#endif