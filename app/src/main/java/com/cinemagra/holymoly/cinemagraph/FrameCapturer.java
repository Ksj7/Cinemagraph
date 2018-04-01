package com.cinemagra.holymoly.cinemagraph;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

/**
 * https://github.com/nbadal/android-gif-encoder/blob/master/GifEncoder.java
 */
public class FrameCapturer {
    private File videoFile;
    private Uri videoFileUri;
    private MediaMetadataRetriever retriever;
    private ArrayList<Bitmap> bitmapArrayList;
    private MediaPlayer mediaPlayer;
    private Thread thread;

    private Handler handler;

    public void run(Context context, Uri uri, int[] position, final Handler handler) {
        this.handler = handler;

        videoFileUri = uri; // Uri.parse(videoFile.toString());
        // instance 생성
        retriever = new MediaMetadataRetriever();
        // 추출할 bitmap 을 담을 array 생성
        bitmapArrayList = new ArrayList<Bitmap>();
        // 사용할 data source의 경로를 설정해준다.
        retriever.setDataSource(context, uri);
        //retriever.setDataSource(videoFile.toString());

        // video file의 총 재생시간을 얻어오기위함
        mediaPlayer = MediaPlayer.create(context, videoFileUri);
        // 동영상 길이
        int millisecond = mediaPlayer.getDuration();

        WindowManager wm = (WindowManager) context.getSystemService(context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        Log.d("Window Size : ", "W:" + width + ", H:" + height);
        Bitmap backgroundBitmap = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST);

        int bgdWidth = backgroundBitmap.getWidth();
        int bgdHeight = backgroundBitmap.getHeight();
        // If Added API 27 , you can use 'getScaledFrameAtTime'method.
        // Bitmap backgroundBitmap = retriever.getScaledFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST, width, height);


        //width : backWidth = position[0] : x
        // width * x = backWidth * position[0]
        // x = (backWidth * position[0] )/width
        double left = (position[0] * bgdWidth) / width + 11;
        double right = (position[1] * bgdWidth) / width + 11;
        double top = (position[2] * bgdHeight) / height + 42;
        double down = (position[3] * bgdHeight) / height + 42;
        position[0] = (int) Math.round(left);
        position[1] = (int) Math.round(right);
        position[2] = (int) Math.round(top);
        position[3] = (int) Math.round(down);
        for (int i = 1000; i < millisecond; i += 1000) {
            // getFrameAtTime 함수는 i 라는 타임에 bitmap을 얻어와준다.(
            // getFrameAtTime의 첫번째 인자의 unit 은 microsecond이다.
            // 그래서 1000을 곱해주었다.
            Bitmap bitmap = retriever.getFrameAtTime(i * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
            // Bitmap bitmap = retriever.getFrameAtTime(i,
            //        MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            Bitmap temp = copyPixelForBitmap(backgroundBitmap);
            // Bitmap temp = backgroundBitmap;
            for (int y = position[2]; y < position[3]; y++) {
                for (int x = position[0]; x < position[1]; x++) {
                    temp.setPixel(x, y, bitmap.getPixel(x, y));
                }
            }
            bitmapArrayList.add(temp);
        }

        // retreiver를 다 사용했다면 release를 해주어야한다.
        retriever.release();
        // Thread start
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                convertGIF(bitmapArrayList);
                handler.sendMessage(Message.obtain(handler, MainActivity.MyHandler.MSG_FINISHED));
            }
        });

        handler.sendMessage(Message.obtain(handler, MainActivity.MyHandler.MSG_STARTED));
        thread.start();
    }

    private Bitmap copyPixelForBitmap(Bitmap copy) {
        return Bitmap.createBitmap(copy);
    }

    private void convertGIF(ArrayList<Bitmap> bitmapArrayList) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        encoder.start(bos);

        for (Bitmap bitmap : bitmapArrayList) {
            sendProgress(bitmapArrayList.size() - 1, bitmapArrayList.indexOf(bitmap));
            encoder.addFrame(bitmap);
        }
        encoder.finish();

        FileOutputStream outStream = null;

        String folder = Environment.getExternalStorageDirectory().toString();
        File saveFolder = new File(folder + "/");
        if (!saveFolder.exists()) {
            saveFolder.mkdirs();
        }

        try {
            outStream = new FileOutputStream(saveFolder.getAbsolutePath() + "/test.gif");
            outStream.write(bos.toByteArray());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendProgress(int total, int current) {
        handler.sendMessage(
                handler.obtainMessage(
                        MainActivity.MyHandler.MSG_PROGRESS, total, current));
    }
}