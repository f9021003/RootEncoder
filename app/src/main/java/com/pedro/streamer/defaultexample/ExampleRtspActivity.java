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

package com.pedro.streamer.defaultexample;

import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.pedro.common.ConnectChecker;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.library.rtsp.RtspCamera1;
import com.pedro.streamer.R;
import com.pedro.streamer.utils.PathUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * More documentation see:
 * {@link com.pedro.library.base.Camera1Base}
 * {@link com.pedro.library.rtsp.RtspCamera1}
 */
public class ExampleRtspActivity extends AppCompatActivity
    implements ConnectChecker, View.OnClickListener, SurfaceHolder.Callback {

  private RtspCamera1 rtspCamera1;
  private Button button;
  private Button bRecord;
  private EditText etUrl;

  private String currentDateAndTime = "";
  private File folder;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    setContentView(R.layout.activity_example);
    folder = PathUtils.getRecordPath();
    SurfaceView surfaceView = findViewById(R.id.surfaceView);
    button = findViewById(R.id.b_start_stop);
    button.setOnClickListener(this);
    bRecord = findViewById(R.id.b_record);
    bRecord.setOnClickListener(this);
    Button switchCamera = findViewById(R.id.switch_camera);
    switchCamera.setOnClickListener(this);
    etUrl = findViewById(R.id.et_rtp_url);
    etUrl.setHint(R.string.hint_rtsp);
    rtspCamera1 = new RtspCamera1(surfaceView, this);
    rtspCamera1.getStreamClient().setReTries(10);
    surfaceView.getHolder().addCallback(this);
  }

  @Override
  public void onConnectionStarted(@NotNull String url) {
  }

  @Override
  public void onConnectionSuccess() {
    Toast.makeText(ExampleRtspActivity.this, "Connection success", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onConnectionFailed(@NonNull final String reason) {
    if (rtspCamera1.getStreamClient().reTry(5000, reason, null)) {
      Toast.makeText(ExampleRtspActivity.this, "Retry", Toast.LENGTH_SHORT)
          .show();
    } else {
      Toast.makeText(ExampleRtspActivity.this, "Connection failed. " + reason, Toast.LENGTH_SHORT)
          .show();
      rtspCamera1.stopStream();
      button.setText(R.string.start_button);
    }
  }

  @Override
  public void onNewBitrate(final long bitrate) {

  }

  @Override
  public void onDisconnect() {
    Toast.makeText(ExampleRtspActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onAuthError() {
    Toast.makeText(ExampleRtspActivity.this, "Auth error", Toast.LENGTH_SHORT).show();
    rtspCamera1.stopStream();
    button.setText(R.string.start_button);
  }

  @Override
  public void onAuthSuccess() {
    Toast.makeText(ExampleRtspActivity.this, "Auth success", Toast.LENGTH_SHORT).show();
  }

  @Override
  public void onClick(View view) {
    int id = view.getId();
    if (id == R.id.b_start_stop) {
      if (!rtspCamera1.isStreaming()) {
        if (rtspCamera1.isRecording()
                || rtspCamera1.prepareAudio() && rtspCamera1.prepareVideo()) {
          button.setText(R.string.stop_button);
          rtspCamera1.startStream(etUrl.getText().toString());
        } else {
          Toast.makeText(this, "Error preparing stream, This device cant do it",
                  Toast.LENGTH_SHORT).show();
        }
      } else {
        button.setText(R.string.start_button);
        rtspCamera1.stopStream();
      }
    } else if (id == R.id.switch_camera) {
      try {
        rtspCamera1.switchCamera();
      } catch (CameraOpenException e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
      }
    } else if (id == R.id.b_record) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        if (!rtspCamera1.isRecording()) {
          try {
            if (!folder.exists()) {
              folder.mkdir();
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            currentDateAndTime = sdf.format(new Date());
            if (!rtspCamera1.isStreaming()) {
              if (rtspCamera1.prepareAudio() && rtspCamera1.prepareVideo()) {
                rtspCamera1.startRecord(
                        folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
                bRecord.setText(R.string.stop_record);
                Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
              } else {
                Toast.makeText(this, "Error preparing stream, This device cant do it",
                        Toast.LENGTH_SHORT).show();
              }
            } else {
              rtspCamera1.startRecord(
                      folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
              bRecord.setText(R.string.stop_record);
              Toast.makeText(this, "Recording... ", Toast.LENGTH_SHORT).show();
            }
          } catch (IOException e) {
            rtspCamera1.stopRecord();
            PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
            bRecord.setText(R.string.start_record);
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
          }
        } else {
          rtspCamera1.stopRecord();
          PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
          bRecord.setText(R.string.start_record);
          Toast.makeText(this,
                  "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
                  Toast.LENGTH_SHORT).show();
        }
      } else {
        Toast.makeText(this, "You need min JELLY_BEAN_MR2(API 18) for do it...",
                Toast.LENGTH_SHORT).show();
      }
    }
  }

  @Override
  public void surfaceCreated(SurfaceHolder surfaceHolder) {

  }

  @Override
  public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
    rtspCamera1.startPreview();
  }

  @Override
  public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && rtspCamera1.isRecording()) {
      rtspCamera1.stopRecord();
      PathUtils.updateGallery(this, folder.getAbsolutePath() + "/" + currentDateAndTime + ".mp4");
      bRecord.setText(R.string.start_record);
      Toast.makeText(this,
          "file " + currentDateAndTime + ".mp4 saved in " + folder.getAbsolutePath(),
          Toast.LENGTH_SHORT).show();
      currentDateAndTime = "";
    }
    if (rtspCamera1.isStreaming()) {
      rtspCamera1.stopStream();
      button.setText(getResources().getString(R.string.start_button));
    }
    rtspCamera1.stopPreview();
  }
}