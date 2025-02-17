/*
 * Copyright (C) 2023 pedroSG94.
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

package com.pedro.library.generic

import android.content.Context
import android.media.MediaCodec
import android.os.Build
import androidx.annotation.RequiresApi
import com.pedro.common.AudioCodec
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.library.base.StreamBase
import com.pedro.library.util.sources.AudioManager
import com.pedro.library.util.sources.VideoManager
import com.pedro.library.util.streamclient.GenericStreamClient
import com.pedro.library.util.streamclient.RtmpStreamClient
import com.pedro.library.util.streamclient.RtspStreamClient
import com.pedro.library.util.streamclient.SrtStreamClient
import com.pedro.library.util.streamclient.StreamClientListener
import com.pedro.rtmp.rtmp.RtmpClient
import com.pedro.rtsp.rtsp.RtspClient
import com.pedro.srt.srt.SrtClient
import java.nio.ByteBuffer

/**
 * Created by pedro on 14/3/22.
 *
 * If you use VideoManager.Source.SCREEN/AudioManager.Source.INTERNAL. Call
 * changeVideoSourceScreen/changeAudioSourceInternal is necessary to start it.
 */

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class GenericStream(
  context: Context,
  private val connectChecker: ConnectChecker,
  videoSource: VideoManager.Source,
  audioSource: AudioManager.Source
): StreamBase(context, videoSource, audioSource) {

  private val streamClientListener = object: StreamClientListener {
    override fun onRequestKeyframe() {
      requestKeyframe()
    }
  }
  private val rtmpClient = RtmpClient(connectChecker)
  private val rtspClient = RtspClient(connectChecker)
  private val srtClient = SrtClient(connectChecker)
  private val streamClient = GenericStreamClient(
    RtmpStreamClient(rtmpClient, streamClientListener),
    RtspStreamClient(rtspClient, streamClientListener),
    SrtStreamClient(srtClient, streamClientListener)
  )
  private var connectedType = ClientType.NONE

  constructor(context: Context, connectChecker: ConnectChecker):
      this(context, connectChecker, VideoManager.Source.CAMERA2, AudioManager.Source.MICROPHONE)

  override fun getStreamClient(): GenericStreamClient = streamClient

  override fun setVideoCodecImp(codec: VideoCodec) {
    rtmpClient.setVideoCodec(codec)
    rtspClient.setVideoCodec(codec)
    srtClient.setVideoCodec(codec)
  }

  override fun setAudioCodecImp(codec: AudioCodec) {
    rtspClient.setAudioCodec(codec)
  }

  override fun audioInfo(sampleRate: Int, isStereo: Boolean) {
    rtmpClient.setAudioInfo(sampleRate, isStereo)
    rtspClient.setAudioInfo(sampleRate, isStereo)
    srtClient.setAudioInfo(sampleRate, isStereo)
  }

  override fun rtpStartStream(endPoint: String) {
    streamClient.connecting(endPoint)
    if (endPoint.startsWith("rtmp", ignoreCase = true)) {
      connectedType = ClientType.RTMP
      startStreamRtpRtmp(endPoint)
    } else if (endPoint.startsWith("rtsp", ignoreCase = true)) {
      connectedType = ClientType.RTSP
      startStreamRtpRtsp(endPoint)
    } else if (endPoint.startsWith("srt", ignoreCase = true)) {
      connectedType = ClientType.SRT
      startStreamRtpSrt(endPoint)
    } else {
      connectChecker.onConnectionFailed("unsupported protocol. Only support rtmp, rtsp and srt")
    }
  }

  private fun startStreamRtpRtmp(endPoint: String) {
    val resolution = super.getVideoResolution()
    rtmpClient.setVideoResolution(resolution.width, resolution.height)
    rtmpClient.setFps(super.getVideoFps())
    rtmpClient.connect(endPoint)
  }

  private fun startStreamRtpRtsp(endPoint: String) {
    rtspClient.connect(endPoint)
  }

  private fun startStreamRtpSrt(endPoint: String) {
    srtClient.connect(endPoint)
  }

  override fun rtpStopStream() {
    when (connectedType) {
      ClientType.RTMP -> rtmpClient.disconnect()
      ClientType.RTSP -> rtspClient.disconnect()
      ClientType.SRT -> srtClient.disconnect()
      else -> {}
    }
    connectedType = ClientType.NONE
  }

  override fun getAacDataRtp(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    when (connectedType) {
      ClientType.RTMP -> rtmpClient.sendAudio(aacBuffer, info)
      ClientType.RTSP -> rtspClient.sendAudio(aacBuffer, info)
      ClientType.SRT -> srtClient.sendAudio(aacBuffer, info)
      else -> {}
    }
  }

  override fun onSpsPpsVpsRtp(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {
    when (connectedType) {
      ClientType.RTMP -> rtmpClient.setVideoInfo(sps, pps, vps)
      ClientType.RTSP -> rtspClient.setVideoInfo(sps, pps, vps)
      ClientType.SRT -> srtClient.setVideoInfo(sps, pps, vps)
      else -> {}
    }
  }

  override fun getH264DataRtp(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    when (connectedType) {
      ClientType.RTMP -> rtmpClient.sendVideo(h264Buffer, info)
      ClientType.RTSP -> rtspClient.sendVideo(h264Buffer, info)
      ClientType.SRT -> srtClient.sendVideo(h264Buffer, info)
      else -> {}
    }
  }
}