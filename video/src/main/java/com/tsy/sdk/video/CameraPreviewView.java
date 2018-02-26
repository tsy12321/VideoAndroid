package com.tsy.sdk.video;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;

/**
 * Camera预览View
 * Created by tsy on 2016/12/29.
 */

public class CameraPreviewView extends FrameLayout {

    private CameraView mCameraView;     //surfarce view
    private Camera mCamera;
    private int mWidth;     //指定视频尺寸宽
    private int mHeight;    //指定视频尺寸高

    private Animation mFocusAnimation;      //对焦动画
    private ImageView mFocusAnimationView;  //对焦资源

    public CameraPreviewView(Context context) {
        this(context, null);
    }

    public CameraPreviewView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreviewView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 添加对焦动画视图
        mFocusAnimationView = new ImageView(context);
        mFocusAnimationView.setVisibility(INVISIBLE);
        mFocusAnimationView.setImageResource(R.drawable.video_focus_icon);
        addView(mFocusAnimationView, new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        // 定义对焦动画
        mFocusAnimation = AnimationUtils.loadAnimation(context, R.anim.focus_animation);
        mFocusAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mFocusAnimationView.setVisibility(INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    /**
     * 初始化
     * @param camera
     * @param width
     * @param height
     */
    public void init(Camera camera, int width, int height) {
        mCamera = camera;
        mWidth = width;
        mHeight = height;

        if(mCameraView != null) {
            removeView(mCameraView);
            mCameraView = null;
        }

        //重新创建一个CameraView加进去
        mCameraView = new CameraView(getContext());
        addView(mCameraView, 0);

        requestLayout();
    }

    public void switchCamera(Camera camera) {
        mCamera = camera;
        mCameraView.switchCamera();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if(mCamera == null) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }
        //按照设置的视频尺寸比例设置显示部分层
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = (int) (width / (1f * mWidth / mHeight));
        int wms = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int hms = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        super.onMeasure(wms, hms);
    }

    public class CameraView extends SurfaceView implements SurfaceHolder.Callback {

        private String TAG = "CameraView";
        private float mOldDist = 1f;

        public CameraView(Context context) {
            super(context);
            getHolder().addCallback(this);
        }

        private void initCamera() {
            getHolder().addCallback(this);
            mCamera.stopPreview();

            Camera.Parameters parameters = mCamera.getParameters();
            Camera.Size previewSize = CameraHelper.getFullScreenPreviewSize(getContext(), mCamera);
//            Log.d(TAG, "previewSize " + previewSize.width + "/" + previewSize.height);
            parameters.setPreviewSize(previewSize.width, previewSize.height);
            mCamera.setParameters(parameters);

            // 预览尺寸改变，请求重新布局、计算宽高
            requestLayout();

            // Set the holder (which might have changed) again
            try {
                mCamera.setPreviewDisplay(getHolder());
                mCamera.startPreview();
            } catch (Exception e) {
                Log.e(TAG, "Could not set preview display in surfaceChanged");
            }

            //对焦一次
            handleFocusMetering(CameraPreviewView.this.getWidth() / 2f, CameraPreviewView.this.getHeight() / 2f);
        }

        public void switchCamera() {
            initCamera();
        }

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
//            Log.d(TAG, "surfaceCreated: ");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
//            Log.d(TAG, "surfaceChanged: ");

            initCamera();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
//            Log.d(TAG, "surfaceDestroyed: ");
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            if(mCamera == null) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                return;
            }
            //基于父view 和预览View比例绘制大小
            Camera.Size size = mCamera.getParameters().getPreviewSize();
            float ratio = 1f * size.height / size.width;
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = (int) (width / ratio);
            int wms = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            int hms = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            super.onMeasure(wms, hms);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if(mCamera == null) {
                return false;
            }
            if (event.getPointerCount() == 1) {     //单点触控
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        handleFocusMetering(event.getX(), event.getY());       //对焦
                        break;
                }
            } else {        //多点触控
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mOldDist = getFingerSpacing(event);          //记录down时的两指距离
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float newDist = getFingerSpacing(event);        //判断是放大还是缩小
                        if (newDist > mOldDist) {
                            handleZoom(true, mCamera);
                        } else if (newDist < mOldDist) {
                            handleZoom(false, mCamera);
                        }
                        mOldDist = newDist;
                        break;
                }
            }
            return true;
        }

        //触摸对焦 测光
        private void handleFocusMetering(final float x, final float y) {
            Camera.Parameters params = mCamera.getParameters();
            Camera.Size previewSize = params.getPreviewSize();
            Rect focusRect = calculateTapArea(x, y, 1f, previewSize);
            Rect meteringRect = calculateTapArea(x, y, 1.5f, previewSize);

            mCamera.cancelAutoFocus();

            //触摸对焦
            if (params.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> focusAreas = new ArrayList<>();
                focusAreas.add(new Camera.Area(focusRect, 800));
                params.setFocusAreas(focusAreas);
            } else {
                Log.w(TAG, "focus areas not supported");
            }

            //触摸测光
            if (params.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> meteringAreas = new ArrayList<>();
                meteringAreas.add(new Camera.Area(meteringRect, 800));
                params.setMeteringAreas(meteringAreas);
            } else {
                Log.w(TAG, "metering areas not supported");
            }

            final String currentFocusMode = params.getFocusMode();
            CameraHelper.setCameraFocusMode(Camera.Parameters.FOCUS_MODE_MACRO, mCamera);

            mCamera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    CameraHelper.setCameraFocusMode(currentFocusMode, mCamera);     //恢复之前的对焦模式
                }
            });

            //对焦动画
            mFocusAnimation.cancel();
            mFocusAnimationView.clearAnimation();
            int left = (int) (x - mFocusAnimationView.getWidth() / 2f);
            int top = (int) (y - mFocusAnimationView.getHeight() / 2f);
            int right = left + mFocusAnimationView.getWidth();
            int bottom = top + mFocusAnimationView.getHeight();
            mFocusAnimationView.layout(left, top, right, bottom);
            mFocusAnimationView.setVisibility(VISIBLE);
            mFocusAnimationView.startAnimation(mFocusAnimation);
        }

        //计算触碰区域
        private Rect calculateTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
            float focusAreaSize = 300;
            int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
            int centerX = (int) (x / previewSize.width - 1000);
            int centerY = (int) (y / previewSize.height - 1000);

            int left = clamp(centerX - areaSize / 2, -1000, 1000);
            int top = clamp(centerY - areaSize / 2, -1000, 1000);

            RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);

            return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
        }

        private int clamp(int x, int min, int max) {
            if (x > max) {
                return max;
            }
            if (x < min) {
                return min;
            }
            return x;
        }

        //获取2个手指的直线距离
        private float getFingerSpacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }

        //处理放大或缩小
        private void handleZoom(boolean isZoomIn, Camera camera) {
            Camera.Parameters params = camera.getParameters();
            if (params.isZoomSupported()) {
                int maxZoom = params.getMaxZoom();
                int zoom = params.getZoom();
                if (isZoomIn && zoom < maxZoom) {
                    zoom++;
                } else if (zoom > 0) {
                    zoom--;
                }
                params.setZoom(zoom);
                camera.setParameters(params);
            } else {
                Log.w(TAG, "zoom not supported");
            }
        }
    }
}
