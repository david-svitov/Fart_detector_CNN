package com.example.fartdetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.View;

public class SpectrogramView extends View {
    private Bitmap spectrogramBitmap = null;
    private Paint paint;

    public SpectrogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (spectrogramBitmap != null) {
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(
                    spectrogramBitmap,
                    this.getWidth(),
                    this.getHeight(),
                    false);
            canvas.drawBitmap(resizedBitmap, 0, 0, paint);
        }
    }

    public void setBitmap(Bitmap newSpectrogram) {
        spectrogramBitmap = newSpectrogram;
    }

    public void setSpectrogram(float[][] newSpectrogram) {
        spectrogramBitmap = specshow(newSpectrogram);
    }

    private Bitmap specshow(float[][] spectrogram) {

        float[][] normalizedSpec = LibrosaUtils.normalizeSpectrogramMinMax(spectrogram);

        Bitmap spectrogramBitmap = Bitmap.createBitmap(
                normalizedSpec[0].length,
                normalizedSpec.length, Bitmap.Config.ARGB_8888);

        for (int i = 0; i < normalizedSpec.length; i++) {
            for (int j = 0; j < normalizedSpec[i].length; j++) {
                int value = (int) (normalizedSpec[i][j] * 255);
                spectrogramBitmap.setPixel(j, i, Color.rgb(0, 0, value));
            }
        }
        return spectrogramBitmap;
    }
}

