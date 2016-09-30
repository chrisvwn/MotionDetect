package com.dssk.jerzy.motiondetect.filters.tilt;

/**
 * Created by chris on 8/8/16.
 */

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.dssk.jerzy.motiondetect.filters.Filter;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.atomic.AtomicBoolean;

public final class TiltFilter implements Filter, SensorEventListener {

    private SensorManager mSensorManager;
    Sensor accelerometer;
    boolean acc;
    private Context cntxt;

    float xAxis, yAxis, zAxis;
    float rollThreshold, pitchThreshold;

    private Paint linePaint;
    Paint dText;

    public TiltFilter(Context context, float prefRollThreshold, float prefPitchThreshold) {
        cntxt = context;

        mSensorManager = (SensorManager) cntxt.getSystemService(Context.SENSOR_SERVICE);

        accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        acc = mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);

        rollThreshold = prefRollThreshold/10;

        pitchThreshold = prefPitchThreshold/10;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {  }

    float[] mGravity;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (mGravity != null) {
            xAxis = mGravity[0];
            yAxis = mGravity[1];
            zAxis = mGravity[2];
        }
    }

    @Override
    public void apply(final Mat src, final Mat dst, AtomicBoolean doesNothing) {

        double threshold = 0.2;

        src.copyTo(dst);

        float x = 0, y = dst.height()/5, yX = dst.height()/10, yY = yX+20, yZ = yY+20;
        int font = 0;
        double fontSize = 0.5;

        if (xAxis >= 0 &&
                yAxis >= rollThreshold*-1 && yAxis <= rollThreshold &&
                zAxis >= pitchThreshold*-1 && zAxis <= pitchThreshold)
            Imgproc.line(dst,new Point(0, dst.height()/2),new Point(dst.width(),dst.height()/2), new Scalar(0,255,0));
        else
            Imgproc.line(dst,new Point(0, dst.height()/2),new Point(dst.width(),dst.height()/2), new Scalar(255,0,0));

        if (mGravity != null) {
            //Imgproc.putText(dst, "x: " + xAxis + " y: " + yAxis + " z: " + zAxis, new Point(x, y), font, fontSize, new Scalar(0, 255, 0));

            if (xAxis < 0) {
                Imgproc.putText(dst, "X: UPSIDE DOWN", new Point(x, yX), font, fontSize, new Scalar(255, 0, 0));
            }
            else
                Imgproc.putText(dst, "X: OK", new Point(x, yX), font, fontSize, new Scalar(0, 255, 0));

            if (yAxis > rollThreshold)
            {
                Imgproc.putText(dst, "Y: Tilt left", new Point(x, yY), font, fontSize, new Scalar(255, 0, 0));
            }
            else if (yAxis < rollThreshold*-1)
            {
                Imgproc.putText(dst, "Y: Tilt right", new Point(x, yY), font, fontSize, new Scalar(255, 0, 0));
            }
            else
            {
                Imgproc.putText(dst, "Y: OK", new Point(x, yY), font, fontSize, new Scalar(0, 255, 0));
            }

            if (zAxis > pitchThreshold)
            {
                Imgproc.putText(dst, "Z: Tilt Back", new Point(x, yZ), font, fontSize, new Scalar(255, 0, 0));
            }
            else if (zAxis < pitchThreshold*-1)
            {
                Imgproc.putText(dst, "Z: Tilt Forwrard", new Point(x, yZ), font, fontSize, new Scalar(255, 0, 0));
            }
            else
            {
                Imgproc.putText(dst, "Z: OK", new Point(x, yZ), font, fontSize, new Scalar(0, 255, 0));
            }
        }
        else
            Imgproc.putText(dst, "Accelerator not found", new Point(x,y),font, fontSize, new Scalar(255,0,0));

    }
}
