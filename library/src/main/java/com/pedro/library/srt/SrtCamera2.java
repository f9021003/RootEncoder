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

package com.pedro.library.srt;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Build;
import android.view.SurfaceView;
import android.view.TextureView;

import androidx.annotation.RequiresApi;

import com.pedro.common.AudioCodec;
import com.pedro.common.ConnectChecker;
import com.pedro.common.VideoCodec;
import com.pedro.library.base.Camera2Base;
import com.pedro.library.util.streamclient.SrtStreamClient;
import com.pedro.library.util.streamclient.StreamClientListener;
import com.pedro.library.view.LightOpenGlView;
import com.pedro.library.view.OpenGlView;
import com.pedro.srt.srt.SrtClient;

import java.nio.ByteBuffer;

/**
 * More documentation see:
 * {@link Camera2Base}
 *
 * Created by pedro on 8/9/23.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SrtCamera2 extends Camera2Base {

  private final SrtClient srtClient;
  private final SrtStreamClient streamClient;
  private final StreamClientListener streamClientListener = this::requestKeyFrame;

  /**
   * @deprecated This view produce rotations problems and could be unsupported in future versions.
   * Use {@link Camera2Base#Camera2Base(OpenGlView)} or {@link Camera2Base#Camera2Base(LightOpenGlView)}
   * instead.
   */
  @Deprecated
  public SrtCamera2(SurfaceView surfaceView, ConnectChecker connectChecker) {
    super(surfaceView);
    srtClient = new SrtClient(connectChecker);
    streamClient = new SrtStreamClient(srtClient, streamClientListener);
  }

  /**
   * @deprecated This view produce rotations problems and could be unsupported in future versions.
   * Use {@link Camera2Base#Camera2Base(OpenGlView)} or {@link Camera2Base#Camera2Base(LightOpenGlView)}
   * instead.
   */
  @Deprecated
  public SrtCamera2(TextureView textureView, ConnectChecker connectChecker) {
    super(textureView);
    srtClient = new SrtClient(connectChecker);
    streamClient = new SrtStreamClient(srtClient, streamClientListener);
  }

  public SrtCamera2(OpenGlView openGlView, ConnectChecker connectChecker) {
    super(openGlView);
    srtClient = new SrtClient(connectChecker);
    streamClient = new SrtStreamClient(srtClient, streamClientListener);
  }

  public SrtCamera2(LightOpenGlView lightOpenGlView, ConnectChecker connectChecker) {
    super(lightOpenGlView);
    srtClient = new SrtClient(connectChecker);
    streamClient = new SrtStreamClient(srtClient, streamClientListener);
  }

  public SrtCamera2(Context context, boolean useOpengl, ConnectChecker connectChecker) {
    super(context, useOpengl);
    srtClient = new SrtClient(connectChecker);
    streamClient = new SrtStreamClient(srtClient, streamClientListener);
  }

  @Override
  public SrtStreamClient getStreamClient() {
    return streamClient;
  }

  @Override
  protected void setVideoCodecImp(VideoCodec codec) {
      srtClient.setVideoCodec(codec);
  }

  @Override
  protected void setAudioCodecImp(AudioCodec codec) {
    if (codec != AudioCodec.AAC) throw new IllegalArgumentException("Unsupported codec: " + codec.name());
  }

  @Override
  protected void prepareAudioRtp(boolean isStereo, int sampleRate) {
    srtClient.setAudioInfo(sampleRate, isStereo);
  }

  @Override
  protected void startStreamRtp(String url) {
    srtClient.connect(url);
  }

  @Override
  protected void stopStreamRtp() {
    srtClient.disconnect();
  }

  @Override
  protected void getAacDataRtp(ByteBuffer aacBuffer, MediaCodec.BufferInfo info) {
    srtClient.sendAudio(aacBuffer, info);
  }

  @Override
  protected void onSpsPpsVpsRtp(ByteBuffer sps, ByteBuffer pps, ByteBuffer vps) {
    srtClient.setVideoInfo(sps, pps, vps);
  }

  @Override
  protected void getH264DataRtp(ByteBuffer h264Buffer, MediaCodec.BufferInfo info) {
    srtClient.sendVideo(h264Buffer, info);
  }
}

