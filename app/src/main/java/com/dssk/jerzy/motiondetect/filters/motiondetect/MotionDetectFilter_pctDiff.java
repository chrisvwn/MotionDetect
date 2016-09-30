package com.dssk.jerzy.motiondetect.filters.motiondetect;

/**
 * Created by chris on 8/8/16.
 */

import com.dssk.jerzy.motiondetect.filters.Filter;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.opencv.core.CvType.CV_8UC1;

public final class MotionDetectFilter_pctDiff implements Filter {

    Mat origScene;
    double origTime;
    double changeDelay = 5000;
    int diffThresh = 0;
    float pctDiff = 0;
    float countDiff = 0;

    //constructor
    public MotionDetectFilter_pctDiff()
    {
        origTime = 0;
        origScene = new Mat();
    }

    private Mat sceneHasChanged(Mat img1, Mat img2)
    {
        Mat diffImage = new Mat();
        Core.absdiff(img1, img2, diffImage);

        Mat foregroundMask = Mat.zeros(diffImage.height(), diffImage.width(), CV_8UC1);

        float threshold = 30.0f;
        double dist;

        byte[] sourceBuffer = new byte[(int)diffImage.total() * diffImage.channels()];
        diffImage.get(0, 0, sourceBuffer);

        int cols = diffImage.width();
        int rows = diffImage.height();

        for(int y=0; y < rows; y++)
            for(int x=0; x<cols; x++)
            {
                double [] pix = diffImage.get(y, x);

                dist = (pix[0]*pix[0] + pix[1]*pix[1] + pix[2]*pix[2]);
                dist = Math.sqrt(dist);

                if(dist>threshold)
                {
                    for (int chanId = 0; chanId < 3; chanId++) {
                        sourceBuffer[y*cols+x*chanId] = (byte)255;
                    }
                    countDiff++;
                }
            }

        pctDiff = countDiff/diffImage.total()*100;

        foregroundMask.put(0,0,sourceBuffer);
        Imgproc.rectangle(foregroundMask, new Point(0,foregroundMask.height()/5-10), new Point(150, foregroundMask.height()/5+30),new Scalar(255,255,255),-1);
        Imgproc.putText(foregroundMask, "cntdiff: "+countDiff, new Point(0, foregroundMask.height()/5), 0,0.5,new Scalar(0,255,0));
        Imgproc.putText(foregroundMask, "pctdiff: "+pctDiff, new Point(0, foregroundMask.height()/5+20), 0, 0.5,new Scalar(0,255,0));
        countDiff = 0;

        return foregroundMask;
    }

    @Override
    public void apply(final Mat src, final Mat dst, AtomicBoolean doesNothing) {

        double currTime = System.currentTimeMillis();

        if (origTime == 0) {
            src.copyTo(origScene);

            origTime = currTime;
        }

        if (currTime - origTime < changeDelay)
            return;

        Mat diff = sceneHasChanged(origScene, src);

        diff.copyTo(dst);
    }
}
