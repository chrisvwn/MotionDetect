package com.dssk.jerzy.motiondetect.filters;

import org.opencv.core.Mat;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by chris on 8/8/16.
 */
public interface Filter {
    public abstract void apply(final Mat src, final Mat dst, AtomicBoolean passBooleanValueBack);
}
