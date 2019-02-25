/*
 *  Copyright (c) 2014 The WebRTC project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#ifndef WEBRTC_MODULES_AUDIO_PROCESSING_CHANNEL_BUFFER_H_
#define WEBRTC_MODULES_AUDIO_PROCESSING_CHANNEL_BUFFER_H_

#include <string.h>

#include "webrtc/base/checks.h"
#include "webrtc/base/gtest_prod_util.h"
#include "webrtc/base/scoped_ptr.h"
#include "webrtc/common_audio/include/audio_util.h"

namespace webrtc {

// Helper to encapsulate a contiguous data buffer, full or split into frequency
// bands, with access to a pointer arrays of the deinterleaved channels and
// bands. The buffer is zero initialized at creation.
//
// The buffer structure is showed below for a 2 channel and 2 bands case:
//
// |data_|:
// { [ --- b1ch1 --- ] [ --- b2ch1 --- ] [ --- b1ch2 --- ] [ --- b2ch2 --- ] }
//
// The pointer arrays for the same example are as follows:
//
// |channels_|:
// { [ b1ch1* ] [ b1ch2* ] [ b2ch1* ] [ b2ch2* ] }
//
// |bands_|:
// { [ b1ch1* ] [ b2ch1* ] [ b1ch2* ] [ b2ch2* ] }
template <typename T>
class ChannelBuffer {
 public:
  ChannelBuffer(size_t num_frames,
                size_t num_channels,
                size_t num_bands = 1)
      : data_(new T[num_frames * num_channels]()),
        channels_(new T*[num_channels * num_bands]),
        bands_(new T*[num_channels * num_bands]),
        num_frames_(num_frames),
        num_frames_per_band_(num_frames / num_bands),
        num_channels_(num_channels),
        num_bands_(num_bands) {
    for (size_t i = 0; i < num_channels_; ++i) {
      for (size_t j = 0; j < num_bands_; ++j) {
        channels_[j * num_channels_ + i] =
            &data_[i * num_frames_ + j * num_frames_per_band_];
        bands_[i * num_bands_ + j] = channels_[j * num_channels_ + i];
      }
    }
  }

  // Returns a pointer array to the full-band channels (or lower band channels).
  // Usage:
  // channels()[channel][sample].
  // Where:
  // 0 <= channel < |num_channels_|
  // 0 <= sample < |num_frames_|
  T* const* channels() { return channels(0); }
  const T* const* channels() const { return channels(0); }

  // Returns a pointer array to the channels for a specific band.
  // Usage:
  // channels(band)[channel][sample].
  // Where:
  // 0 <= band < |num_bands_|
  // 0 <= channel < |num_channels_|
  // 0 <= sample < |num_frames_per_band_|
  const T* const* channels(size_t band) const {
    RTC_DCHECK_LT(band, num_bands_);
    return &channels_[band * num_channels_];
  }
  T* const* channels(size_t band) {
    const ChannelBuffer<T>* t = this;
    return const_cast<T* const*>(t->channels(band));
  }

  // Returns a pointer array to the bands for a specific channel.
  // Usage:
  // bands(channel)[band][sample].
  // Where:
  // 0 <= channel < |num_channels_|
  // 0 <= band < |num_bands_|
  // 0 <= sample < |num_frames_per_band_|
  const T* const* bands(size_t channel) const {
    RTC_DCHECK_LT(channel, num_channels_);
    RTC_DCHECK_GE(channel, 0u);
    return &bands_[channel * num_bands_];
  }
  T* const* bands(size_t channel) {
    const ChannelBuffer<T>* t = this;
    return const_cast<T* const*>(t->bands(channel));
  }

  // Sets the |slice| pointers to the |start_frame| position for each channel.
  // Returns |slice| for convenience.
  const T* const* Slice(T** slice, size_t start_frame) const {
    RTC_DCHECK_LT(start_frame, num_frames_);
    for (size_t i = 0; i < num_channels_; ++i)
      slice[i] = &channels_[i][start_frame];
    return slice;
  }
  T** Slice(T** slice, size_t start_frame) {
    const ChannelBuffer<T>* t = this;
    return const_cast<T**>(t->Slice(slice, start_frame));
  }

  size_t num_frames() const { return num_frames_; }
  size_t num_frames_per_band() const { return num_frames_per_band_; }
  size_t num_channels() const { return num_channels_; }
  size_t num_bands() const { return num_bands_; }
  size_t size() const {return num_frames_ * num_channels_; }

  void SetDataForTesting(const T* data, size_t size) {
    RTC_CHECK_EQ(size, this->size());
    memcpy(data_.get(), data, size * sizeof(*data));
  }

 private:
  rtc::scoped_ptr<T[]> data_;
  rtc::scoped_ptr<T* []> channels_;
  rtc::scoped_ptr<T* []> bands_;
  const size_t num_frames_;
  const size_t num_frames_per_band_;
  const size_t num_channels_;
  const size_t num_bands_;
};

// One int16_t and one float ChannelBuffer that are kept in sync. The sync is
// broken when someone requests write access to either ChannelBuffer, and
// reestablished when someone requests the outdated ChannelBuffer. It is
// therefore safe to use the return value of ibuf_const() and fbuf_const()
// until the next call to ibuf() or fbuf(), and the return value of ibuf() and
// fbuf() until the next call to any of the other functions.
class IFChannelBuffer {
 public:
  IFChannelBuffer(size_t num_frames, size_t num_channels, size_t num_bands = 1);

  ChannelBuffer<int16_t>* ibuf();
  ChannelBuffer<float>* fbuf();
  const ChannelBuffer<int16_t>* ibuf_const() const;
  const ChannelBuffer<float>* fbuf_const() const;

  size_t num_frames() const { return ibuf_.num_frames(); }
  size_t num_frames_per_band() const { return ibuf_.num_frames_per_band(); }
  size_t num_channels() const { return ibuf_.num_channels(); }
  size_t num_bands() const { return ibuf_.num_bands(); }

 private:
  void RefreshF() const;
  void RefreshI() const;

  mutable bool ivalid_;
  mutable ChannelBuffer<int16_t> ibuf_;
  mutable bool fvalid_;
  mutable ChannelBuffer<float> fbuf_;
};

}  // namespace webrtc

#endif  // WEBRTC_MODULES_AUDIO_PROCESSING_CHANNEL_BUFFER_H_
