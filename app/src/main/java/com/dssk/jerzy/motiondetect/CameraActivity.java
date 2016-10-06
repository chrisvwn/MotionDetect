package com.dssk.jerzy.motiondetect;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.dssk.jerzy.motiondetect.filters.*;
import com.dssk.jerzy.motiondetect.filters.NoneFilter;
import com.dssk.jerzy.motiondetect.filters.tilt.TiltFilter;
import com.dssk.jerzy.motiondetect.filters.motiondetect.MotionDetectFilter;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.preference.PreferenceFragment;

import javax.mail.*;

// Use the deprecated Camera class.
@SuppressWarnings("deprecation")
public final class CameraActivity extends AppCompatActivity
        implements CvCameraViewListener2 {

    // A tag for log output.
    private static final String TAG =
            CameraActivity.class.getSimpleName();

    // A key for storing the index of the active camera.
    private static final String STATE_CAMERA_INDEX = "cameraIndex";

    // A key for storing the index of the active image size.
    private static final String STATE_IMAGE_SIZE_INDEX =
            "imageSizeIndex";

    // Keys for storing the indices of the active filters.
    private static final String STATE_TILT_FILTER_INDEX =
            "convolutionFilterIndex";
    private static final String STATE_MOTION_DETECT_FILTER_INDEX =
            "motionDetectFilterIndex";

    // An ID for items in the image size submenu.
    private static final int MENU_GROUP_ID_SIZE = 2;

    // The filters.
    private Filter[] mTiltFilters;
    private Filter[] mMotionDetectFilters;

    // The indices of the active filters.
    private int mTiltFilterIndex;
    private int mMotionDetectFilterIndex;

    // The index of the active camera.
    private int mCameraIndex;

    // The index of the active image size.
    private int mImageSizeIndex;

    // Whether the active camera is front-facing.
    // If so, the camera view should be mirrored.
    private boolean mIsCameraFrontFacing;

    // The number of cameras on the device.
    private int mNumCameras;

    // The image sizes supported by the active camera.
    private List<Size> mSupportedImageSizes;

    // The camera view.
    private CameraBridgeViewBase mCameraView;

    // Whether the next camera frame should be saved as a photo.
    private boolean mIsPhotoPending;

    // A matrix that is used when saving photos.
    private Mat mBgr;

    // Whether an asynchronous menu action is in progress.
    // If so, menu interaction should be disabled.
    private boolean mIsMenuLocked;

    private AtomicBoolean motionDetected = new AtomicBoolean(false);

    MarshMallowPermission marshMallowPermission = new MarshMallowPermission(this);

    List<String> addressList = new ArrayList<String>();

    SharedPreferences prefs;
    String prefSmtpHost;
    String prefSmtpPort;
    String prefSmtpUser;
    String prefSmtpPass;

    String prefEmailAddress;

    int prefStartDelay;
    int prefDetectedDelay;
    int prefSizeThresh;

    int prefTiltRollThresh;
    int prefTiltPitchThresh;


    SendMail sender;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.e("OPENCV LOADER", "  OpenCVLoader.initDebug(), not working.");
        }
        else
        {
            Log.d("OPENCV LOADER", "  OpenCVLoader.initDebug(), working.");

            //System.loadLibrary("my_jni_lib1");
            //System.loadLibrary("my_jni_lib2");
        }
    }

    /*
    // The OpenCV loader callback.

    private BaseLoaderCallback mLoaderCallback =
            new BaseLoaderCallback(this) {
                @Override
                public void onManagerConnected(final int status) {
                    switch (status) {
                        case LoaderCallbackInterface.SUCCESS:
                            Log.d(TAG, "OpenCV loaded successfully");
                            mCameraView.enableView();
                            //mCameraView.enableFpsMeter();
                            mBgr = new Mat();

                            break;
                        default:
                            super.onManagerConnected(status);
                            break;
                    }
                }
            };
*/


    // Suppress backward incompatibility errors because we provide
    // backward-compatible fallbacks.
    @SuppressLint("NewApi")
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (savedInstanceState != null) {
            mCameraIndex = savedInstanceState.getInt(
                    STATE_CAMERA_INDEX, 0);
            mImageSizeIndex = savedInstanceState.getInt(
                    STATE_IMAGE_SIZE_INDEX, 0);
            mTiltFilterIndex = savedInstanceState.getInt(
                    STATE_TILT_FILTER_INDEX, 0);
            mMotionDetectFilterIndex = savedInstanceState.getInt(
                    STATE_MOTION_DETECT_FILTER_INDEX, 0);
        } else {
            mCameraIndex = 0;
            mImageSizeIndex = 0;
            mTiltFilterIndex = 0;
            mMotionDetectFilterIndex = 0;

            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            prefSmtpHost = prefs.getString("pref_smtp_host", "smtp.gmail.com");
            prefSmtpPort = prefs.getString("pref_smtp_port", "467");
            prefSmtpUser = prefs.getString("pref_smtp_user", "motdetapp@gmail.com");
            prefSmtpPass = prefs.getString("pref_smtp_pass", "password123*");

            prefEmailAddress = prefs.getString("pref_email_address", "");

            prefStartDelay = Integer.parseInt(prefs.getString("pref_start_delay", "5"));
            prefDetectedDelay = Integer.parseInt(prefs.getString("pref_detected_delay", "30"));
            prefSizeThresh = Integer.parseInt(prefs.getString("pref_size_threshold", "5"));

            prefTiltRollThresh = Integer.parseInt(prefs.getString("pref_pitch_tilt_threshold", "2"));
            prefTiltPitchThresh = Integer.parseInt(prefs.getString("pref_pitch_tilt_threshold", "2"));

            sender = new SendMail(prefSmtpUser, prefSmtpPass, addressList, "Motion Detected", "");
        }

        //final Camera camera;
        Camera camera = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            CameraInfo cameraInfo = new CameraInfo();
            Camera.getCameraInfo(mCameraIndex, cameraInfo);
            mIsCameraFrontFacing =
                    (cameraInfo.facing ==
                            CameraInfo.CAMERA_FACING_FRONT);
            mNumCameras = Camera.getNumberOfCameras();

            if (!marshMallowPermission.checkPermissionForCamera()) {
                marshMallowPermission.requestPermissionForCamera();
            }
            else
            {
                if (!marshMallowPermission.checkPermissionForExternalStorage())
                {
                    marshMallowPermission.requestPermissionForExternalStorage();
                }
                else
                {
                    try {
                        camera = Camera.open(mCameraIndex);
                    }
                    catch(Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }

        } else { // pre-Gingerbread
            // Assume there is only 1 camera and it is rear-facing.
            mIsCameraFrontFacing = false;
            mNumCameras = 1;
            camera = Camera.open();
        }
        if (marshMallowPermission.checkPermissionForCamera()) {
            final Parameters parameters = camera.getParameters();
            camera.release();
            mSupportedImageSizes =  parameters.getSupportedPreviewSizes();

            //fix only 1 camera size
            int idx = 0;

            while (mSupportedImageSizes.size() > 1)
            {
                final Size size = mSupportedImageSizes.get(idx);

                if (size.width == 800 && size.height == 480)
                    idx++;
                else
                    mSupportedImageSizes.remove(idx);
            }

            final Size size = mSupportedImageSizes.get(mImageSizeIndex);

            mCameraView = new JavaCameraView(this, mCameraIndex);
            mCameraView.setMaxFrameSize(size.width, size.height);
            mCameraView.setCvCameraViewListener(this);
            mCameraView.disableFpsMeter();
            mCameraView.enableView();
            //mCameraView.enableFpsMeter();
            mBgr = new Mat();
            setContentView(mCameraView);
         }
        else
        {
            dispMessage(CameraActivity.this, "Please enable camera permissions");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the current camera index.
        savedInstanceState.putInt(STATE_CAMERA_INDEX, mCameraIndex);

        // Save the current image size index.
        savedInstanceState.putInt(STATE_IMAGE_SIZE_INDEX,
                mImageSizeIndex);

        // Save the current filter indices.
        savedInstanceState.putInt(STATE_TILT_FILTER_INDEX,
                mTiltFilterIndex);
        savedInstanceState.putInt(STATE_MOTION_DETECT_FILTER_INDEX,
                mMotionDetectFilterIndex);
        super.onSaveInstanceState(savedInstanceState);
    }

    // Suppress backward incompatibility errors because we provide
    // backward-compatible fallbacks.
    @SuppressLint("NewApi")
    @Override
    public void recreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            super.recreate();
        } else {
            finish();
            startActivity(getIntent());
        }
    }

    @Override
    public void onPause() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
            Log.e("OPENCV LOADER", " Resume OpenCVLoader.initDebug(), not working.");
        }
        else
        {
            Log.d("OPENCV LOADER", " Resume OpenCVLoader.initDebug(), working.");
            if(mCameraView != null)
                mCameraView.enableView();


            //System.loadLibrary("my_jni_lib1");
            //System.loadLibrary("my_jni_lib2");
        }

        mIsMenuLocked = false;
    }

    @Override
    public void onDestroy() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.activity_camera, menu);
        //if (mNumCameras < 2) {
        if (mNumCameras < 3) { //temporarily remove this option
            // Remove the option to switch cameras, since there is
            // only 1.
            //menu.removeItem(R.id.menu_next_camera);
        }
        int numSupportedImageSizes = mSupportedImageSizes.size();

        if (numSupportedImageSizes > 1) {
            final SubMenu sizeSubMenu = menu.addSubMenu(
                    R.string.menu_image_size);
            for (int i = 0; i < numSupportedImageSizes; i++) {
                final Size size = mSupportedImageSizes.get(i);
                sizeSubMenu.add(MENU_GROUP_ID_SIZE, i, Menu.NONE,
                        String.format("%dx%d", size.width, size.height));
            }
        }
        return true;
    }

    // Suppress backward incompatibility errors because we provide
    // backward-compatible fallbacks (for recreate).
    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (mIsMenuLocked) {
            return true;
        }
        if (item.getGroupId() == MENU_GROUP_ID_SIZE) {
            mImageSizeIndex = item.getItemId();
            recreate();

            return true;
        }
        switch (item.getItemId()) {
            case R.id.menu_tilt:
                mTiltFilterIndex++;

                if (mTiltFilterIndex == 1)
                {
                    prefTiltRollThresh = Integer.parseInt(prefs.getString("pref_roll_tilt_threshold", "2"));
                    prefTiltPitchThresh = Integer.parseInt(prefs.getString("pref_pitch_tilt_threshold", "2"));

                    mTiltFilters = new Filter[] {
                            new NoneFilter(),
                            new TiltFilter(this, prefTiltRollThresh, prefTiltPitchThresh),
                    };
                    return true;
                }
                if (mTiltFilterIndex ==mTiltFilters.length) {
                    mTiltFilterIndex = 0;
                    mTiltFilters = null;
                }
                return true;

            case R.id.menu_start_motion_detect:
                mMotionDetectFilterIndex++;

                if (mMotionDetectFilterIndex == 1)
                {
                    prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefStartDelay = Integer.parseInt(prefs.getString("pref_start_delay", "5"));
                    prefDetectedDelay = Integer.parseInt(prefs.getString("pref_detected_delay", "30"));
                    prefSizeThresh = Integer.parseInt(prefs.getString("pref_size_threshold", "5"));


                    mMotionDetectFilters = new Filter[] {
                            new NoneFilter(),
                            new MotionDetectFilter(prefStartDelay, prefDetectedDelay, prefSizeThresh),
                    };

                    dispMessage(CameraActivity.this, "Starting motion detection");
                }

                if (mMotionDetectFilterIndex == mMotionDetectFilters.length) {
                    mMotionDetectFilterIndex = 0;
                    mMotionDetectFilters = null;

                    dispMessage(CameraActivity.this, "Stopping motion detection");

                }
                return true;

            case R.id.menu_preferences:
                // Open the photo in LabActivity.
                final Intent intent = new Intent(this, UserSettingsActivity.class);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(intent);
                    }
                });
                return true;
/*            case R.id.menu_next_camera:
                mIsMenuLocked = true;

                // With another camera index, recreate the activity.
                mCameraIndex++;
                if (mCameraIndex == mNumCameras) {
                    mCameraIndex = 0;
                }
                recreate();

                return true;
            case R.id.menu_take_photo:
                mIsMenuLocked = true;

                // Next frame, take the photo.
                mIsPhotoPending = true;

                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.activity_camera, container, false);
            return rootView;
        }

    } //end fragment

    @Override
    public void onCameraViewStarted(final int width,
                                    final int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(final CvCameraViewFrame inputFrame) {
        final Mat rgba = inputFrame.rgba();

        // Apply the active filters.
        if (mTiltFilters != null) {
           mTiltFilters[mTiltFilterIndex].apply(
                    rgba, rgba, motionDetected);
        }

        if (mMotionDetectFilters != null) {
            mMotionDetectFilters[mMotionDetectFilterIndex].apply(
                    rgba, rgba, motionDetected);
        }

        if (motionDetected.get())
            mIsPhotoPending = true;

        if (mIsPhotoPending) {
            mIsPhotoPending = false;
            takePhoto(rgba);
        }

        if (mIsCameraFrontFacing) {
            // Mirror (horizontally flip) the preview.
            Core.flip(rgba, rgba, 1);
        }

        return rgba;
    }

    private void dispMessage(final Context context, final String msg)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, msg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void takePhoto(final Mat rgba) {

        // Determine the path and metadata for the photo.
        final long currentTimeMillis = System.currentTimeMillis();
        final String appName = getString(R.string.app_name);
        final String galleryPath =
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES).toString();
        final String albumPath = galleryPath + File.separator +
                appName;
        final String photoPath = albumPath + File.separator +
                currentTimeMillis + LabActivity.PHOTO_FILE_EXTENSION;
        final ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(Images.Media.MIME_TYPE,
                LabActivity.PHOTO_MIME_TYPE);
        values.put(Images.Media.TITLE, appName);
        values.put(Images.Media.DESCRIPTION, appName);
        values.put(Images.Media.DATE_TAKEN, currentTimeMillis);

        // Ensure that the album directory exists.
        File album = new File(albumPath);
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Failed to create album directory at " +
                    albumPath);
            onTakePhotoFailed();
            return;
        }

        // Try to create the photo.
        Imgproc.cvtColor(rgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        if (!Imgcodecs.imwrite(photoPath, mBgr)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }

        dispMessage(CameraActivity.this, "Photo saved successfully to " + photoPath);

        Log.d(TAG, "Photo saved successfully to " + photoPath);

        // Try to insert the photo into the MediaStore.
        Uri uri;
        try {
            uri = getContentResolver().insert(
                    Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to insert photo into MediaStore");
            e.printStackTrace();

            // Since the insertion failed, delete the photo.
            File photo = new File(photoPath);
            if (!photo.delete()) {
                Log.e(TAG, "Failed to delete non-inserted photo");
            }

            onTakePhotoFailed();
            return;
        }


        if (addressList.isEmpty())
        {
            prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String pref_email_address = prefs.getString("pref_email_address", "chrisvwn@yahoo.co.uk");
            addressList.add(pref_email_address);
        }

        sender.setBody(photoPath);

        new AsyncTask<Void, Void, Void>() {
            @Override public Void doInBackground(Void... arg) {
                try {
                    dispMessage(CameraActivity.this, "Sending mail to:" + addressList.toString());

                    sender.sendEmail();
                } catch (Exception e) {
                    final String err = e.getMessage();

                    dispMessage(CameraActivity.this, "Failed sending email: " + err);

                    Log.e("SendMail", e.getMessage(), e);
                }
                return null;
            }
        }.execute();

/*
        // Open the photo in LabActivity.
        final Intent intent = new Intent(this, LabActivity.class);
        intent.putExtra(LabActivity.EXTRA_PHOTO_URI, uri);
        intent.putExtra(LabActivity.EXTRA_PHOTO_DATA_PATH,
                photoPath);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startActivity(intent);
            }
        });
*/
    }

    private void onTakePhotoFailed() {
        mIsMenuLocked = false;

        // Show an error message.
        final String errorMessage = getString(R.string.photo_error_message);

        dispMessage(CameraActivity.this, errorMessage);
    }
}