package com.cinemagra.holymoly.cinemagraph;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_WRITE_STORAGE = 112;
    private static final int REQUEST_READ_STORAGE = 111;
    private MySurfaceView surfaceView;
    private DrawRectangle drawRectangle;
    private FrameLayout layout;
    private Uri uri = null;
    private String path;
    private int deviceWidth;
    private int deviceHeight;
    private int customizedWidth;
    private int customizedHeight;
    private Bitmap thumb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        layout = findViewById(R.id.layout);
        drawRectangle = findViewById(R.id.circleFrame);
        boolean hasWritePermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        boolean hasReadPermission = (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED);


        DisplayMetrics disp = getResources().getDisplayMetrics();
        deviceWidth = disp.widthPixels;
        deviceHeight = disp.heightPixels;

        if (!hasWritePermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_STORAGE);
        }
        if (!hasReadPermission) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_READ_STORAGE);

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
        thumb = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Images.Thumbnails.MINI_KIND);
        layout.addView(surfaceView, 0);
        if (thumb.getWidth() >= thumb.getHeight()) {
            customizedHeight = thumb.getHeight() * (deviceWidth / thumb.getWidth());
            customizedWidth = deviceWidth;
        } else {
            customizedWidth = thumb.getWidth() * (deviceHeight / thumb.getHeight());
            customizedHeight = deviceHeight;
        }
        layout.setLayoutParams(new ConstraintLayout.LayoutParams(customizedWidth, customizedHeight));
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                if (data.getData() == null) {
                    Log.d(getClass().getSimpleName(), "selected video path = null!");
                    uri = null;
                } else {
                    uri = data.getData();
                    path = getPathFromUri(uri);
                    processSurfaceView(uri);
                }
            }
        }
    }

    public void onClickChangePage(View view) {
        Intent i = new Intent(getApplicationContext(), GifViewActivity.class);
        startActivity(i);
    }

    //gif make 버튼
    public void onClickMakeBtn(View view) {
        if (uri == null) {
            Toast.makeText(this, "error!", Toast.LENGTH_SHORT).show();
            return;
        }
        Point[] points = drawRectangle.points;
        int[] position = new int[4];// left, top, right, bottom;

        int left, top, right, bottom;
        left = points[0].x;
        top = points[0].y;
        right = points[0].x;
        bottom = points[0].y;
        for (int i = 1; i < points.length; i++) {
            left = left > points[i].x ? points[i].x : left;
            top = top > points[i].y ? points[i].y : top;
            right = right < points[i].x ? points[i].x : right;
            bottom = bottom < points[i].y ? points[i].y : bottom;
        }
        position[0] = left;
        position[1] = right;
        position[2] = top;
        position[3] = bottom;
        MyHandler handler = new MyHandler();
        FrameCapturer frameCapturer = new FrameCapturer();

        try {
            frameCapturer.run(getApplicationContext(), uri, position, handler, customizedWidth, customizedHeight);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String getPathFromUri(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        assert cursor != null;
        cursor.moveToNext();
        String path = cursor.getString(cursor.getColumnIndex("_data"));
        cursor.close();
        return path;
    }

    public class MyHandler extends Handler {
        static final int MSG_STARTED = 1;
        static final int MSG_PROGRESS = 2;
        static final int MSG_FINISHED = 3;

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_STARTED) {
                ProgressBar progressBar = findViewById(R.id.progressBar);
                progressBar.setVisibility(View.VISIBLE);
            } else if (msg.what == MSG_PROGRESS) {
                int total = msg.arg1;
                int current = msg.arg2;

                ProgressBar horitontalProgressBar = findViewById(R.id.progressBar2);

                if (current == 0) {
                    ProgressBar progressBar = findViewById(R.id.progressBar);
                    progressBar.setVisibility(View.GONE);

                    horitontalProgressBar.setVisibility(View.VISIBLE);
                    horitontalProgressBar.setProgressBackgroundTintMode(PorterDuff.Mode.DARKEN);
                    horitontalProgressBar.setMax(total);
                }

                horitontalProgressBar.setProgress(current);

            } else if (msg.what == MSG_FINISHED) {
                ProgressBar horitontalProgressBar = findViewById(R.id.progressBar2);
                horitontalProgressBar.setVisibility(View.GONE);

                Toast.makeText(MainActivity.this, "FINISHED!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //reload my activity with permission granted or use the features what required the permission
                } else {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }
            case REQUEST_READ_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //reload my activity with permission granted or use the features what required the permission
                } else {
                    Toast.makeText(this, "The app was not allowed to read to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }
        }

    }
}
