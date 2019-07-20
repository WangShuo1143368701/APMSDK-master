
#include <jni.h>
#include <stdlib.h>
#include <assert.h>
#include <stddef.h>
#include <unistd.h>

#include <android/log.h>
#define TAG "-------- APM --------"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)

#include "webrtc/modules/audio_processing/include/audio_processing.h"
#include "webrtc/modules/include/module_common_types.h"
#include "webrtc/common_audio/channel_buffer.h"
#include "webrtc/common_audio/include/audio_util.h"
#include "webrtc/common.h"

using namespace std;
using namespace webrtc;

    class ApmProcessing {

    private :
    protected:
    public:
        ApmProcessing();
    protected:
         bool isInit = false;
         int sample_rate_hz = 48000;
         int num_input_channels = 1;

         int reverse_sample_rate_hz = 48000;
         int num_reverse_channels = 1;

        AudioFrame *_frame;
        AudioFrame *_reverseFrame;
        unique_ptr<ChannelBuffer<float>> _float_cb;
        unique_ptr<ChannelBuffer<float>> _revfloat_cb;
    public:
        unique_ptr<AudioProcessing> _apm;

        int Init(int sample_rate ,int channels);

        int ProcessStream(int16_t* data);

        int ProcessReverseStream(int16_t* data);

        int Release();

    };


