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

package com.pedro.streamer.texturemodeexample;

import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pedro.common.ConnectChecker;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.streamer.R;
import com.pedro.library.rtmp.RtmpCamera2;
import com.pedro.library.view.AutoFitTextureView;
import com.pedro.streamer.utils.PathUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * More documentation see:
 * {@link com.pedro.library.base.Camera2Base}
 * {@link com.pedro.library.rtmp.RtmpCamera2}
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class TextureModeRtmpActivity extends AppCompatActivity
    implements ConnectChecker, View.OnClickListener, TextureView.SurfaceTextureListener {

  private RtmpCamera2 rtmpCamera2;
  private AutoFitTextureView textureView;
  private Button button;
  private Button bRecord;
  private EditText etUrl;

  private String currentDateAndTime = "";
  private File folder;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.activity_texture_mode);
    folder = PathUtils.getRecordPath();
    textureView = findViewById(R.id.textureView);
    button = findViewById(R.id.b_start_stop);
    button.setOnClickListener(this);
    bRecord = findViewById(R.id.b_record);
    bRecord.setOnClickListener(this);
    Button switchCamera = findViewById(R.id.switch_camera);
    switchCamera.setOnClickListener(this);
    etUrl = findViewById(R.id.et_rtp_url);
    etUrl.setHint(R.string.hint_rtmp);
    rtmpCamera2 = new RtmpCamera2(textureView, this);
    textureView.setSurfaceTextureListener(this);
  }

  @Override
  public void onConnectionStarted(@NonNull String url) {
  }

  @Override
  public void onConnectionSuccess() {
    Toast.makeText(TextureModeRtmpActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onConnectionFailed(@NonNull final String reason) {
    Toast.makeText(TextureModeRtmpActivity.this, "Connection failed. " + reason,
        Toast.LENGTH_SHORT).show();
    rtmpCamera2.stopStream();
    button.setText(R.string.start_button);
  }

  @Override
  public void onNewBitrate(long bitrate) {

  }

  @Override
  public void onDisconnect() {
    Toast.makeText(TextureModeRtmpActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onAuthError() {
    Toast.makeText(TextureModeRtmpActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onAuthSuccess() {
    Toast.makeText(TextureModeRtmpActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onClick(View view) {
    int id = view.getId();
    if (id == R.id.b_start_stop) {
      if (!rtmpCamera2.isStreaming()) {
        if (rtmpCamera2.isRecording()
                || rtmpCamera2.prepareAudio() && rtmpCamera2.prepareVideo()) {
          button.setText(R.string.stop_button);
          rtmpCamera2.startStream(etUrl.getText().toString());
        } else {
          Toast.makeText(this, "Error preparing stream, This device cant do it",
                  Toast.LENGTH_SHORT).show();
        }
      } else {
        button.setText(R.string.start_button);
        rtmpCamera2.stopStream();
      }
    } else if (id == R.id.switch_camera) {
      try {
        rtmpCamera2.switchCamera();
      } catch (CameraOpenException e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
      }
    } else if (id == R.id.b_record) {
      if (!rtmpCamera2.isRecording()) {
        try {
          if (!folder.exists()) {
            folder.mkdir();
          }
          SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
          currentDateAndTime = sdf.format(new Date());
          if (!rtmpCamera2.isStreaming()) {
            if (rtmpCamera2.prepareAudio() && rtmpCamera2.prepareVideo()) {
              rtmpCamera2.startRecord(
                      folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
              bRecord.setText(R.string.stop_record);
              Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
            } else {
              Toast.makeText(this, "Error preparing stream, This device cant do it",
                      Toast.LENGTH_SHORT).show();
            }
          } else {
            rtmpCamera2.startRecord(folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
            bRecord.setText(R.string.stop_record);
            Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
          }
        } catch (IOException e) {
          rtmpCamera2.stopRecord();
          PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
          bRecord.setText(R.string.start_record);
          Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
      } else {
        rtmpCamera2.stopRecord();
        PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
        bRecord.setText(R.string.start_record);
        Toast.makeText(this,
                "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                Toast.LENGTH_SHORT).show();
        currentDateAndTime = "";
      }
    }
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
    textureView.setAspectRatio(480, 640);
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
    rtmpCamera2.startPreview();
  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
    if (rtmpCamera2.isRecording()) {
      rtmpCamera2.stopRecord();
      PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
      bRecord.setText(R.string.start_record);
      Toast.makeText(this,
          "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
          Toast.LENGTH_SHORT).show();
      currentDateAndTime = "";
    }
    if (rtmpCamera2.isStreaming()) {
      rtmpCamera2.stopStream();
      button.setText(getResources().getString(R.string.start_button));
    }
    rtmpCamera2.stopPreview();

    return true;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

  }
}