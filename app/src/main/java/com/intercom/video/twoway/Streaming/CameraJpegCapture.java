package com.intercom.video.twoway.Streaming;

/*
This class deals with capturing camera data in real time
 */

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;

import com.intercom.video.twoway.R;
import com.intercom.video.twoway.Utilities.Utilities;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class CameraJpegCapture
{
    public Utilities utilities;
    int pWidth=320;
    int pHeight=240;
    int jpegQuality = 10;
    Camera.Parameters params;

    private Camera mCamera;
    private CameraPreview mPreview;
    FrameLayout preview;
    VideoStreaming streamEngine;
    Audio audioEngine;

    public static Camera.PreviewCallback previewCallback;

    public CameraJpegCapture(VideoStreaming streamer, Audio audio, Utilities utilities)
    {
        this.utilities = utilities;
        streamEngine = streamer;
        audioEngine = audio;
    }

    //returns list of supported preview sizes.  this will be useful later for determining video reslution
    // Also prints them to log (useful now so we can manually set them for testing)
    private List getSupportedPreviewSizes()
    {
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)(utilities.mainContext)).getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int height = metrics.widthPixels;
        int width = metrics.heightPixels;

        Camera.Size result=null;
        Camera.Size size;
        Camera.Parameters p = mCamera.getParameters();
        for(int i=0;i<p.getSupportedPreviewSizes().size();i++)
        {
            size = p.getSupportedPreviewSizes().get(i);

            System.out.println("Supported Preview Size: "+size.width+" "+size.height);
        }
        return p.getSupportedPreviewSizes();
    }

    /*
    we pass in streamEngine so we can send out frames as they are captured
     */
    public void startCam()
    {
        try
        {
            // Create an instance of Camera
            mCamera = getCameraInstance();

            setupPreviewJpegCaptureCallback(mCamera);

            // mCamera = openFrontFacingCameraGingerbread();
            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(utilities.mainContext, mCamera);

            preview = (FrameLayout) ((Activity)(utilities.mainContext)).findViewById(R.id.camera_preview);

            preview.addView(mPreview);
            params = mCamera.getParameters();
            params.setPreviewSize(pWidth, pHeight);

	        mCamera.setDisplayOrientation(90);
            mCamera.setParameters(params);
			mCamera.setPreviewTexture(new SurfaceTexture(0));
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /*
    This sets our callback tat is called on every single camera preview frame
     */
    public void setupPreviewJpegCaptureCallback(Camera c)
    {
        try
        {
            previewCallback = new Camera.PreviewCallback()
            {
                public void onPreviewFrame(byte[] data, Camera camera)
                {
                    Camera.Parameters parameters = camera.getParameters();
                    int imageFormat = parameters.getPreviewFormat();
                    if (imageFormat == ImageFormat.NV21)
                    {
                        YuvImage img = new YuvImage(data, ImageFormat.NV21, pWidth, pHeight, null);

                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        img.compressToJpeg(new Rect(0, 0, pWidth, pHeight), jpegQuality, out);
                        byte[] imageBytes = out.toByteArray();

                        // dont try to send anything if we arent connected
                        if(streamEngine.connected)
                        {
                            streamEngine.sendJpegFrame(imageBytes, audioEngine.consumeAudioBytes());
                        }
                    }
                }

            };
            c.setPreviewCallback(previewCallback);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        return;
    }
    public static Camera getCameraInstance()
    {
        Camera c = null;
        Camera.CameraInfo info = new Camera.CameraInfo();
        int numCameras = Camera.getNumberOfCameras();
        boolean frontCameraOpened = false;

        for (int i = 0; i < numCameras; i ++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    Log.i("CameraJpegCapture", "Attempting to use front-facing camera");
                    c = Camera.open(i);
                    Log.i("CameraJpegCapture", "Front-facing camera set as default");
                    frontCameraOpened = true;
                } catch (RuntimeException e) {
                    Log.e("CameraJpegCapture", "Front-facing camera failed to open");
                }
            }
        }

        if (!frontCameraOpened) {
            Log.i("CameraJpegCapture", "Attempting to use rear-facing camera");
            c = Camera.open();
        }
        if (c != null) {
            Log.i("CameraJpegCapture", "Rear-facing camera set as default");
        } else {
            Log.e("CameraJpegCapture", "Rear-facing camera failed to open");
        }

        return c;
    }
}

