package com.cinemagra.holymoly.cinemagraph;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.GlideDrawableImageViewTarget;

/**
 * Created by sujin.kim on 2018. 3. 29..
 */

public class GifViewActivity extends AppCompatActivity {
    Uri uri;
    ImageView imageView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gif_view);

        imageView = findViewById(R.id.gif_image);
        someMethod();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == 1) {
                if (data.getData() == null) {
                    Log.d(getClass().getSimpleName(), "selected image path = null!");
                    uri = null;
                } else {
                    uri = data.getData();
                    processSurfaceView(uri);
                }
            }
        }
    }

    private void processSurfaceView(final Uri fileUri) {

        GlideDrawableImageViewTarget gifImage = new GlideDrawableImageViewTarget(imageView);
        Glide.with(this).load(fileUri).into(gifImage);
    }

    void someMethod() {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivityForResult(intent, 1);
    }
}