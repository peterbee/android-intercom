package com.intercom.video.twoway;

/*
This class deals with capturing camera data in real time
 */

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class CameraJpegCapture
{
    int pWidth=320;
    int pHeight=240;
    Camera.Parameters params;

    private Camera mCamera;
    private CameraPreview mPreview;
    FrameLayout preview;

    static ImageView jpegImageView;
    VideoStreaming streamEngine;

    static Camera.PreviewCallback previewCallback;

    CameraJpegCapture(VideoStreaming streamer)
    {
        streamEngine = streamer;
    }

    //returns list of supported preview sizes.  this will be useful later for determining video reslution
    // Also prints them to log (useful now so we can manually set them for testing)
    private List getSupportedPreviewSizes()
    {
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity)(MainActivity.utilities.mainContext)).getWindowManager().getDefaultDisplay().getMetrics(metrics);

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
    void startCam()
    {
        try
        {
            // Create an instance of Camera
            mCamera = getCameraInstance();

            setupPreviewJpegCaptureCallback(mCamera);

            // mCamera = openFrontFacingCameraGingerbread();
            // Create our Preview view and set it as the content of our activity.
            mPreview = new CameraPreview(MainActivity.utilities.mainContext, mCamera);

            preview = (FrameLayout) ((Activity)(MainActivity.utilities.mainContext)).findViewById(R.id.camera_preview);

            preview.addView(mPreview);
            params = mCamera.getParameters();
            params.setPreviewSize(pWidth, pHeight);

	        mCamera.setDisplayOrientation(0);
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
    void setupPreviewJpegCaptureCallback(Camera c)
    {
        jpegImageView = (ImageView) ((Activity) (MainActivity.utilities.mainContext)).findViewById(R.id.jpegTestImageView);

        try
        {
            previewCallback = new Camera.PreviewCallback()
            {
                public void onPreviewFrame(byte[] data, Camera camera)
                {
                    android.hardware.Camera.Parameters parameters = camera.getParameters();
                    int imageFormat = parameters.getPreviewFormat();
                    if (imageFormat == ImageFormat.NV21)
                    {
                        YuvImage img = new YuvImage(data, ImageFormat.NV21, pWidth, pHeight, null);

                        ByteArrayOutputStream out = new ByteArrayOutputStream();

                        img.compressToJpeg(new Rect(0, 0, pWidth, pHeight), 80, out);
                        byte[] imageBytes = out.toByteArray();

                        System.out.println("About to send captured jpeg frame!");

                        // dont try to send anything if we arent connected
                        if(streamEngine.connected)
                        {
                            streamEngine.sendJpegFrame(imageBytes);
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

        c = Camera.open();

        return c;
    }
}

