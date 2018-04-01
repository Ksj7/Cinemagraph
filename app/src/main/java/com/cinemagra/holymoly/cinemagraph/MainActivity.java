package com.cinemagra.holymoly.cinemagraph;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_WRITE_STORAGE = 112;
    private MySurfaceView surfaceView;
    private DrawRectangle drawRectangle;
    private DecodeTask decodeTask;
    ConstraintLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout = (ConstraintLayout) findViewById(R.id.layout);
        drawRectangle = (DrawRectangle) findViewById(R.id.circleFrame);
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
        someMethod();
    }


    void someMethod() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, 1);
    }

    private void processSurfaceView(final Uri fileUri) {
        surfaceView = new MySurfaceView(getApplication(), fileUri);
        layout.addView(surfaceView, 0);
    }

    private Uri uri = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                if (data.getData() == null) {
                    Log.d(getClass().getSimpleName(), "selected video path = null!");
                    uri = null;
                } else {
                    uri = data.getData();
                    processSurfaceView(uri);
                }
            }
        }
    }

    public void onClickChangePage(View view){
        Intent i = new Intent(getApplicationContext(),GIFTestActivty.class);
        startActivity(i);
    }
    //gif make 버튼
    public void onClickMakeBtn(View view) {
        if (uri == null) {
            Toast.makeText(this, "error!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (decodeTask != null) {
            decodeTask.interrupt();
        }

        Point[] points = drawRectangle.points;
        int[] position = new int[4];// left, top, right, bottom;

        int left, top, right, bottom;
        left = points[0].x;
        top = points[0].y;
        right = points[0].x;
        bottom = points[0].y;
        for (int i = 1; i < points.length; i++) {
            left = left > points[i].x ? points[i].x:left;
            top = top > points[i].y ? points[i].y:top;
            right = right < points[i].x ? points[i].x:right;
            bottom = bottom < points[i].y ? points[i].y:bottom;
        }
        position[0] = left;
        position[1] = right;
        position[2] = top;
        position[3] = bottom;
        MyHandler handler = new MyHandler();
        FrameCapturer frameCapturer = new FrameCapturer();
        frameCapturer.run(this, uri, position, handler);
    }

    public class MyHandler extends Handler {
        public static final int MSG_STARTED = 1;
        public static final int MSG_PROGRESS = 2;
        public static final int MSG_FINISHED = 3;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_STARTED) {
                ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
            } else if (msg.what == MSG_PROGRESS) {
                int total = msg.arg1;
                int current = msg.arg2;

                ProgressBar horitontalProgressBar = (ProgressBar) findViewById(R.id.progressBar2);

                if (current == 0) {
                    ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.GONE);

                    horitontalProgressBar.setVisibility(View.VISIBLE);
                    horitontalProgressBar.setProgressBackgroundTintMode(PorterDuff.Mode.DARKEN);
                    horitontalProgressBar.setMax(total);
                }

                horitontalProgressBar.setProgress(current);

            } else if (msg.what == MSG_FINISHED) {
                ProgressBar horitontalProgressBar = (ProgressBar) findViewById(R.id.progressBar2);
                horitontalProgressBar.setVisibility(View.GONE);

                Toast.makeText(MainActivity.this, "FINISHED!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    //reload my activity with permission granted or use the features what required the permission
                } else
                {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }
        }

    }
}
