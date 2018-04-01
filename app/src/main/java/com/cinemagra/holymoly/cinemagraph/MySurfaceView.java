package com.cinemagra.holymoly.cinemagraph;

import android.content.Context;
import android.net.Uri;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by sujin.kim on 2017. 11. 23..
 */

class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private Context context;
    private Uri fileUri;
    private DecodeTask decodeTask;

    public MySurfaceView(Context c, Uri uri) {
        super(c);
        context = c;
        fileUri = uri;
        getHolder().addCallback(this);
        setFocusable(true);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (decodeTask != null) {
            return;
        }
        decodeTask = new DecodeTask(context, holder.getSurface(), fileUri);
        decodeTask.start();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        decodeTask.onDestroy();
    }

}