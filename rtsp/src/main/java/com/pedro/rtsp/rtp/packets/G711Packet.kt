/*
 * Copyright (C) 2021 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.rtsp.rtp.packets

import android.media.MediaCodec
import com.pedro.rtsp.rtsp.RtpFrame
import com.pedro.rtsp.utils.RtpConstants
import java.nio.ByteBuffer

/**
 *
 * RFC 7655.
 */
class G711Packet(
  sampleRate: Int
): BasePacket(
  sampleRate.toLong(),
  RtpConstants.payloadTypeG711
) {

  init {
    channelIdentifier = RtpConstants.trackAudio
  }

  override fun createAndSendPacket(
    byteBuffer: ByteBuffer,
    bufferInfo: MediaCodec.BufferInfo,
    callback: (RtpFrame) -> Unit
  ) {
    val length = bufferInfo.size - byteBuffer.position()
    val maxPayload = maxPacketSize - RtpConstants.RTP_HEADER_LENGTH
    var sum = 0
    while (sum < length) {
      val size = if (length - sum < maxPayload) length - sum else maxPayload
      val buffer = getBuffer(size + RtpConstants.RTP_HEADER_LENGTH)
      byteBuffer.get(buffer, RtpConstants.RTP_HEADER_LENGTH, size)
      val ts = bufferInfo.presentationTimeUs * 1000
      markPacket(buffer)
      val rtpTs = updateTimeStamp(buffer, ts)
      updateSeq(buffer)
      val rtpFrame = RtpFrame(buffer, rtpTs, RtpConstants.RTP_HEADER_LENGTH + size , rtpPort, rtcpPort, channelIdentifier)
      sum += size
      callback(rtpFrame)
    }
  }
}