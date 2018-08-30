package com.cinemagra.holymoly.cinemagraph;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.jcodec.api.FrameGrab;
import org.jcodec.api.JCodecException;
import org.jcodec.common.AndroidUtil;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by jeffjeong on 2017. 11. 19..
 * Modified by sujin.kim
 * https://github.com/nbadal/android-gif-encoder/blob/master/GifEncoder.java
 */
public class FrameCapturer {
    private String videoPathName;
    private ArrayList<Bitmap> bitmapArrayList;
    private Thread thread;
    private int dw, dh;
    //private char[] path;
    static {
        System.loadLibrary("gifflen");
    }
    public native int Init(String gifName, int w, int h, int numColors, int quality,
                           int frameDelay);
    public native void Close();
    public native int AddFrame(int[] pixels, boolean isFirst);

    private Handler handler;

    public String getPathFromUri(Context c, Uri uri){
        Cursor cursor = c.getContentResolver().query(uri, null, null, null, null );
        cursor.moveToNext();
        String path = cursor.getString( cursor.getColumnIndex( "_data" ) );
        cursor.close();

        return path;
    }

    public void run(Context context , Uri uri, int[] position, final Handler handler, int width, int height, final int idx) throws IOException, JCodecException {
    //public void run(String path, int[] position, final Handler handler, int width, int height) throws IOException {
        this.handler = handler;
        // 추출할 bitmap 을 담을 array 생성
        bitmapArrayList = new ArrayList<>();
        //path = getPathFromUri(context, uri).toCharArray();

        MediaMetadataRetriever m = new MediaMetadataRetriever();
        m.setDataSource(context, uri);
        long timeInmillisec = Long.parseLong(m.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        long duration = timeInmillisec / 1000;
        long hours = duration / 3600;
        long minutes = (duration - hours * 3600) / 60;
        long seconds = duration - (hours * 3600 + minutes * 60) ;
        int nTime = 1;
        Bitmap backgroundBitmap = null;
        while( nTime <= seconds ) {
            Bitmap bitmap = m.getFrameAtTime(nTime * 1000000);
            dw = Math.round(width / 2);
            dh = Math.round(height / 2);
            Bitmap resized = Bitmap.createScaledBitmap(bitmap, Math.round(width / 2), Math.round(height / 2), true);
            if (backgroundBitmap == null) {
                backgroundBitmap = Bitmap.createBitmap(resized);
                position[0] = Math.round(position[0] / 2);
                position[1] = (int) Math.floor(position[1] / 2);
                position[2] = Math.round(position[2] / 2);
                position[3] = (int) Math.floor(position[3] / 2);
            }
            Bitmap modified = Bitmap.createBitmap(backgroundBitmap);
            for (int y = position[2]; y < position[3]; y++) {
                for (int x = position[0]; x < position[1]; x++) {
                    modified.setPixel(x, y, resized.getPixel(x, y));
                }
            }
            bitmapArrayList.add(modified);
            nTime++;
        }
        // Thread start
        thread = new Thread(new Runnable() {
            @Override
            public void run() {

                if(idx == 2) {
                    long start = System.currentTimeMillis();
                    convertGIFTEST(bitmapArrayList);
                    long end = System.currentTimeMillis();
                    System.out.println("실행 시간 : " + (end - start) / 1000.0);
                }else{
                    long start = System.currentTimeMillis();
                    convertGIF(bitmapArrayList);
                    long end = System.currentTimeMillis();
                    System.out.println( "실행 시간 real : " + ( end - start )/1000.0 );
                }
                handler.sendMessage(Message.obtain(handler, MainActivity.MyHandler.MSG_FINISHED));
            }
        });

        handler.sendMessage(Message.obtain(handler, MainActivity.MyHandler.MSG_STARTED));
        thread.start();
    }
    private void convertGIFTEST(ArrayList<Bitmap> bitmapArrayList) {
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
            outStream = new FileOutputStream(saveFolder.getAbsolutePath() + "/test2.gif");
            outStream.write(bos.toByteArray());
            outStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void convertGIF(ArrayList<Bitmap> bitmapArrayList) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        //AnimatedGifEncoder encoder = new AnimatedGifEncoder();
        //encoder.start(bos);
        // Filename, width, height, colors, quality, frame delay
        String folder = Environment.getExternalStorageDirectory().toString();
        File saveFolder = new File(folder + "/");
        if (!saveFolder.exists()) {
            saveFolder.mkdirs();
        }
        if (Init(saveFolder.getAbsolutePath() + "/test.gif", dw, dh, 256, 100, 4) != 0) {
            Log.e("gifflen", "Init failed");
        }
        boolean isFirst = true;
        for (Bitmap bitmap : bitmapArrayList) {
            sendProgress(bitmapArrayList.size() - 1, bitmapArrayList.indexOf(bitmap));
            int[] pixels = new int[dw*dh];
            bitmap.getPixels(pixels, 0, dw, 0, 0, dw, dh);
            // Convert to 256 colors and add to gif
            AddFrame(pixels, isFirst);
            isFirst = false;
            //encoder.addFrame(bitmap);

        }
        //encoder.finish();
        Close();
//        FileOutputStream outStream = null;
//
//        String folder = Environment.getExternalStorageDirectory().toString();
//        File saveFolder = new File(folder + "/");
//        if (!saveFolder.exists()) {
//            saveFolder.mkdirs();
//        }
//
//        try {
//            outStream = new FileOutputStream(saveFolder.getAbsolutePath() + "/test.gif");
//            outStream.write(bos.toByteArray());
//            outStream.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private void sendProgress(int total, int current) {
        handler.sendMessage(
                handler.obtainMessage(
                        MainActivity.MyHandler.MSG_PROGRESS, total, current));
    }
}