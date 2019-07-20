


#include "apm_processing.h"

void SetFrameSampleRate(AudioFrame* frame,
                        int sample_rate_hz) {
    frame->sample_rate_hz_ = sample_rate_hz;
    frame->samples_per_channel_ = AudioProcessing::kChunkSizeMs *
                                  sample_rate_hz / 1000;
}

template <typename T>
void SetContainerFormat(int sample_rate_hz,
                        size_t num_channels,
                        AudioFrame* frame,
                        unique_ptr<ChannelBuffer<T> >* cb) {
    SetFrameSampleRate(frame, sample_rate_hz);
    frame->num_channels_ = num_channels;
    cb->reset(new ChannelBuffer<T>(frame->samples_per_channel_, num_channels));
}

void ConvertToFloat(const int16_t* int_data, ChannelBuffer<float>* cb) {
    ChannelBuffer<int16_t> cb_int(cb->num_frames(),
                                  cb->num_channels());
    Deinterleave(int_data,
                 cb->num_frames(),
                 cb->num_channels(),
                 cb_int.channels());
    for (size_t i = 0; i < cb->num_channels(); ++i) {
        S16ToFloat(cb_int.channels()[i],
                   cb->num_frames(),
                   cb->channels()[i]);
    }
}

void ConvertToFloat(const AudioFrame& frame, ChannelBuffer<float>* cb) {
    ConvertToFloat(frame.data_, cb);
}

// ----------------------- ApmProcessing  ---------------

ApmProcessing::ApmProcessing(){
    isInit = false;
    sample_rate_hz = AudioProcessing::kSampleRate48kHz;
    num_input_channels = 1;

    reverse_sample_rate_hz = AudioProcessing::kSampleRate48kHz;
    num_reverse_channels = 1;
}

int ApmProcessing::Release(){
    if (isInit){
        delete _frame;
        delete _reverseFrame;
    }
    isInit = false;
    return 0;
}

int ApmProcessing::Init(int sample_rate, int channels){
    sample_rate_hz = sample_rate;
    num_input_channels = channels;
    reverse_sample_rate_hz = sample_rate;
    num_reverse_channels = channels;

    _apm.reset(AudioProcessing::Create());
    _frame = new AudioFrame();
    _reverseFrame = new AudioFrame();

    SetContainerFormat(sample_rate_hz, num_input_channels, _frame, &_float_cb);

    SetContainerFormat(reverse_sample_rate_hz, num_reverse_channels, _reverseFrame,
                       &_revfloat_cb);

    _apm->Initialize({{{_frame->sample_rate_hz_, _frame->num_channels_},
                              {_frame->sample_rate_hz_, _frame->num_channels_},
                              {_reverseFrame->sample_rate_hz_, _reverseFrame->num_channels_},
                              {_reverseFrame->sample_rate_hz_, _reverseFrame->num_channels_}}});
    isInit = true;
    return 0;
}

int ApmProcessing::ProcessStream(int16_t* data){
    if (isInit){
        std::copy(data, data + _frame->samples_per_channel_, _frame->data_);
//        ConvertToFloat(*_frame, _float_cb.get());
        int ret = _apm->ProcessStream(_frame);
        std::copy(_frame->data_, _frame->data_ + _frame->samples_per_channel_, data);
        return ret;
    }
    return -100;
}

int ApmProcessing::ProcessReverseStream(int16_t* data){
    if (isInit){
        std::copy(data, data + _reverseFrame->samples_per_channel_, _reverseFrame->data_);
//        ConvertToFloat(*_reverseFrame, _revfloat_cb.get());
        int ret = _apm->ProcessReverseStream(_reverseFrame);

        return ret;
    }
    return -100;
}




