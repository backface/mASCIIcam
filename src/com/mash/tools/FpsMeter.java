package com.mash.tools;

import java.text.DecimalFormat;

import org.opencv.core.Core;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.Log;

public class FpsMeter {
    private static final String TAG = "mASCIIcam::FpsMeter";
    public static boolean DEBUG = false;
    
    int                         step;
    int                         framesCouner;
    double                      freq;
    long                        prevFrameTime;
    String                      strfps;
    DecimalFormat               twoPlaces = new DecimalFormat("0.00");
    Paint                       paint,bgpaint;

    public void init() {
        step = 20;
        framesCouner = 0;
        freq = Core.getTickFrequency();
        prevFrameTime = Core.getTickCount();
        strfps = "";

        paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(20);
        
        bgpaint = new Paint();
        bgpaint.setStyle(Style.FILL);
        bgpaint.setColor(Color.BLACK);
        
    }

    public void measure() {
        framesCouner++;
        if (framesCouner % step == 0) {
            long time = Core.getTickCount();
            double fps = step * freq / (time - prevFrameTime);
            prevFrameTime = time;
            DecimalFormat twoPlaces = new DecimalFormat("0.00");
            strfps = twoPlaces.format(fps) + " FPS";
            
            if (DEBUG) Log.i(TAG, strfps);
        }
    }

    public void draw(Canvas canvas, float x, float y) {
    	int p = (int) paint.measureText(strfps);
    	canvas.drawRect(x, y - paint.getTextSize(), x + p, y , bgpaint);
        canvas.drawText(strfps, x, y, paint);
    }

}
