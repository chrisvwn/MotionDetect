package com.dssk.jerzy.motiondetect.filters.motiondetect;

/**
 * Created by chris on 8/8/16.
 */

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.dssk.jerzy.motiondetect.CameraActivity;
import com.dssk.jerzy.motiondetect.filters.Filter;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class MotionDetectFilter implements Filter {

    Mat origScene;

    Mat diff;
    double origTime;
    double detectedDelay, startDelay;
    Mat hierarchy = new Mat();
    List<MatOfPoint> cnts = new ArrayList<MatOfPoint>();
    double imgCntArea = 0;
    Scalar colorGreen = new Scalar(0, 255, 0);
    Mat frameDelta = new Mat();
    Mat imgResult = new Mat();
    Mat thresh = new Mat();
    Size kernSize = new Size(21, 21);
    Point txtPos = new Point(0, 50);
    Mat kern = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5));;
    String state = "initializing";
    Point threshCenterAnchor = new Point(-1,-1);
    Size imgRescaleSize = new Size(800, 600);
    Mat gray1 = new Mat();
    Mat gray2 = new Mat();
    AtomicBoolean motion = new AtomicBoolean(false);
    Point topLeft = new Point(), bottomRight = new Point();
    boolean msgDisplayed = false;
    double imgFraction;
    double currTime;

    //constructor
    public MotionDetectFilter(int prefStartDelay, int prefDetectedDelay, int prefSize)
    {
        origTime = 0;
        detectedDelay = prefDetectedDelay*1000;
        startDelay = prefStartDelay*1000; //5000ms
        imgFraction = (float)prefSize/100.0; //0.02 = 2%

        origScene = new Mat();
        diff = new Mat();
    }

    private Mat detectSceneChange(Mat img1, Mat img2)
    {
        if (img1.empty() || img2.empty())
            return img2;

        imgResult = img2.clone();

        Imgproc.cvtColor(img1, gray1, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray1, gray1, kernSize, 0);

        Imgproc.cvtColor(img2, gray2, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(gray2, gray2, kernSize, 0);


        Core.absdiff(gray1, gray2, frameDelta);

        if (frameDelta.empty()) {
            state="error: absdiff empty";
            return img2;
        }

        Imgproc.threshold(frameDelta, thresh, 25, 255, Imgproc.THRESH_BINARY);

        // dilate the thresholded image to fill in holes, then find contours
        // on thresholded image
        //Imgproc.dilate(thresh, thresh, kern, threshCenterAnchor, 2);

        Imgproc.findContours(thresh, cnts, hierarchy, Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_SIMPLE);

        state = "empty";

        if (imgCntArea == 0)
            imgCntArea = Imgproc.contourArea(new MatOfPoint(new Point(0,0),
                new Point(gray1.width(),0),
                new Point(gray1.width(),gray1.height()),
                new Point(0, gray1.height())));

        int cntsSize = cnts.size();

        // loop over the contours
        for (int i=0;i< cntsSize;i++) {
            // if the contour is too small, ignore it
            if (Imgproc.contourArea(cnts.get(0)) / imgCntArea < imgFraction) {
                cnts.remove(0);
                continue;
            }

            Rect bRect = Imgproc.boundingRect(cnts.get(0));
            topLeft.x = bRect.x;
            topLeft.y = bRect.y;
            bottomRight.x = topLeft.x + bRect.width;
            bottomRight.y = topLeft.y + bRect.height;

            Imgproc.rectangle(imgResult, topLeft, bottomRight, colorGreen, 2);

            state = "detected";

            motion.set(true);

            cnts.remove(0);

            origTime = 0;
        }

        Imgproc.putText(imgResult, "Status: "+ state, txtPos, 0,0.5,colorGreen);

        gray1.release();
        gray2.release();
        frameDelta.release();
        thresh.release();
        hierarchy.release();
        //System.gc();

        return imgResult;
    }

    @Override
    public void apply(final Mat src, final Mat dst, AtomicBoolean motionDetected) {

//        src.copyTo(dst);

        currTime = System.currentTimeMillis();

        motion.set(false);
/*
        if (src.width() > 800)
            // load the image, resize it, and convert it to grayscale
            Imgproc.resize(src, src, imgRescaleSize, 0, 0, Imgproc.INTER_AREA);
*/

        if (origTime == 0) {
            src.copyTo(origScene);

            origTime = currTime;

            motion.set(false);

            motionDetected.set(motion.get());
        }

        if (state.contains("initializing") && (currTime - origTime < startDelay))
        {
            state = "initializing "  + (startDelay - (currTime - origTime))/1000;
            Imgproc.putText(dst, "Status: " + state, txtPos, 0, 0.5, colorGreen);
            return;
        }

        if (state.contains("detected") && (currTime - origTime < detectedDelay))
        {
            state = "detected " + (detectedDelay - (currTime - origTime))/1000;
            Imgproc.putText(dst, "Status: " + state, txtPos, 0, 0.5, colorGreen);
            return;
        }

        Imgproc.putText(dst, "Status: " + state, txtPos, 0, 0.5, colorGreen);

        diff = detectSceneChange(origScene, src);

        motionDetected.set(motion.get());

        diff.copyTo(dst);

        diff.release();
    }
}
