package com.hty.screenrecord;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Switch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    Spinner spinner;
    Switch switch1;
    Button button_record, button_open, button_about;
    boolean isRecording = false;
    String filename = "";
    MediaProjectionManager projectionManager;
    MediaProjection mediaProjection;
    MediaRecorder mediaRecorder;
    DisplayMetrics metrics;
    VirtualDisplay virtualDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        metrics= new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        List<String> list = new ArrayList<String>();
        list.add(metrics.widthPixels + " X " + metrics.heightPixels);
        list.add("1080 X 1920");
        list.add("720 X 1280");
        list.add("480 X 800");
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,list);
        adapter.setDropDownViewResource(android.R.layout.simple_list_item_checked);
        spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(adapter);

        switch1 = (Switch) findViewById(R.id.switch1);

        button_record = (Button) findViewById(R.id.button_record);
        button_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecording) {
                    isRecording = false;
                    button_record.setText("开始录屏");
                    stopRecord();
                    button_open.setEnabled(true);
                    MediaScannerConnection.scanFile(MainActivity.this, new String[] { filename }, null, null);
                } else {
                    isRecording = true;
                    button_record.setText("停止录屏");
                    projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
                    Intent captureIntent = projectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, 101);
                }
            }
        });
        if(android.os.Build.VERSION.SDK_INT < 21){
            button_record.setEnabled(false);
            button_record.setText("不支持安卓 5.0 以下版本");
        }

        button_open = (Button) findViewById(R.id.button_open);
        button_open.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!filename.equals("")) {
                    String suffix = MimeTypeMap.getFileExtensionFromUrl(filename);
                    String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(Uri.parse("file://" + filename), type);
                    startActivity(intent);
                }
            }
        });

        button_about = (Button) findViewById(R.id.button_about);
        button_about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).setIcon(R.mipmap.ic_launcher).setTitle("海天鹰录屏 V1.0")
                        .setMessage("安卓 5.0 以上屏幕录制，可设置录制视频的分辨率，是否录音。\n作者：黄颖\nQQ：84429027\n参考：https://github.com/GLGJing/ScreenRecorder")
                        .setPositiveButton("确定", null).show();
            }
        });

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        mediaProjection = projectionManager.getMediaProjection(resultCode, data);
        if (mediaProjection == null) {
            Log.e("onActivityResult", "media projection is null");
            return;
        }
        startRecord();
    }

    void startRecord() {
        button_open.setEnabled(false);
        mediaRecorder = new MediaRecorder();
        if(switch1.isChecked()) {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        }
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        filename = getsaveDirectory() + System.currentTimeMillis() + ".mp4";
        mediaRecorder.setOutputFile(filename);
        String resolution = spinner.getSelectedItem().toString();
        String [] temp = resolution.split(" X ");
        int width = Integer.parseInt(temp[0]);
        int height = Integer.parseInt(temp[1]);
        Log.e("video", width + "," + height);
        mediaRecorder.setVideoSize(width, height);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        if(switch1.isChecked()) {
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        }
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024);
        mediaRecorder.setVideoFrameRate(30);
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        virtualDisplay = mediaProjection.createVirtualDisplay("MainScreen", metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mediaRecorder.getSurface(), null, null);
        mediaRecorder.start();
    }

    void stopRecord() {
        mediaRecorder.stop();
        mediaRecorder.reset();
        virtualDisplay.release();
        mediaProjection.stop();
    }

    String getsaveDirectory() {
        String path = Environment.getExternalStorageDirectory() + "/ScreenRecord/";
        //Log.e("path",path);
        File file = new File(path);
        if(!file.exists()) file.mkdirs();
        return path;
    }
}
