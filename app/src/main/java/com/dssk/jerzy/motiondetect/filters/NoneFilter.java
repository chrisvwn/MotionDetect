package com.dssk.jerzy.motiondetect.filters;

import com.dssk.jerzy.motiondetect.filters.Filter;

import org.opencv.core.Mat;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by chris on 8/8/16.
 */
public class NoneFilter implements Filter {
    public void apply(final Mat src, final Mat dst, AtomicBoolean doesNothing)
    {
        //Do nothing.
    }
}
