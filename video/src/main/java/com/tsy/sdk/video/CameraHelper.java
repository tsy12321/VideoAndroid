package com.tsy.sdk.video;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.DisplayMetrics;
import android.view.Surface;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * CameraHelper
 * Created by tsy on 2016/12/29.
 */

public class CameraHelper {

    private static final String TAG = "CameraHelper";

    /**
     * 初始化摄像头
     * @param cameraFacing
     *          Camera.CameraInfo.CAMERA_FACING_BACK 后置
     *          Camera.CameraInfo.CAMERA_FACING_FRONT 前置
     */
    public static Camera getCamera(Activity activity, int cameraFacing) throws RuntimeException {
        int cameraId = getCameraId(cameraFacing);
        Camera camera = Camera.open(cameraId);
        setCameraDisplayOrientation(activity, cameraId, camera);
        return camera;
    }

    //设置旋转角度
    private static int setCameraDisplayOrientation(Activity activity,
                                                  int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);

        return result;
    }

    //获取指定位置的摄像头id
    private static int getCameraId(int cameraFacing) {
        int numberOfCameras = Camera.getNumberOfCameras();

        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == cameraFacing) {
                return i;
            }
        }

        return -1;
    }

    /**
     * 设置相机对焦模式
     *
     * @param focusMode
     * @param camera
     */
    public static void setCameraFocusMode(String focusMode, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<String> sfm = parameters.getSupportedFocusModes();
        if (sfm.contains(focusMode)) {
            parameters.setFocusMode(focusMode);
        }
        camera.setParameters(parameters);
    }

    /**
     * 获取全屏的预览尺寸
     * @param context
     * @param camera
     * @return
     */
    public static Camera.Size getFullScreenPreviewSize(Context context, Camera camera) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int w_screen = dm.widthPixels;
        int h_screen = dm.heightPixels;


        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

        //降序
        Collections.sort(sizes, new Comparator<Camera.Size>() {

            public int compare(final Camera.Size a, final Camera.Size b) {
                return b.width * b.height - a.width * a.height;
            }
        });

        Camera.Size optimalSize = null;

        for (Camera.Size size : sizes) {
            if(size.width == h_screen && size.height == w_screen) {
                optimalSize = size;
                break;
            }
        }

        if(optimalSize == null) {
            optimalSize = sizes.get(0);         //如果没找到 直接返回最大的
        }

        return optimalSize;
    }
}
