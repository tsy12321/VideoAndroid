package com.ci123.videodemo;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by tsy on 2016/12/30.
 */

public class RecorderCircleView extends FrameLayout {

    private ImageView mOutBgCircle;
    private ImageView mInBgCircle;
    private ImageView mPressedOutBgCircle;
    private ImageView mPressedInBgCircle;
    private ProgressCircleView mProgressCircleView;

    private int mTotalTime = 10000;     //总时间 默认10s
    private long mStartTime = 0L;   //开始时间

    private boolean mIsRecording = false;

    public void setTotalTime(int totalTime) {
        mTotalTime = totalTime;
    }

    public void start() {
        mStartTime = System.currentTimeMillis();
        mIsRecording = true;

        mInBgCircle.setVisibility(GONE);
        mOutBgCircle.setVisibility(GONE);
        mPressedInBgCircle.setVisibility(VISIBLE);
        mPressedOutBgCircle.setVisibility(VISIBLE);

        mProgressCircleView.setVisibility(VISIBLE);
        mProgressCircleView.invalidate();
    }

    public void stop() {
        mIsRecording = false;
        mPressedInBgCircle.setVisibility(GONE);
        mPressedOutBgCircle.setVisibility(GONE);
        mInBgCircle.setVisibility(VISIBLE);
        mOutBgCircle.setVisibility(VISIBLE);

        mProgressCircleView.setVisibility(GONE);
    }

    public RecorderCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mOutBgCircle = new ImageView(getContext());
        mOutBgCircle.setImageResource(R.drawable.shape_recorder_out_bg);
        addView(mOutBgCircle, new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        mPressedOutBgCircle = new ImageView(getContext());
        mPressedOutBgCircle.setImageResource(R.drawable.shape_recorder_out_bg_pressed);
        mPressedOutBgCircle.setVisibility(GONE);
        addView(mPressedOutBgCircle, new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        mInBgCircle = new ImageView(getContext());
        mInBgCircle.setImageResource(R.drawable.shape_recorder_in_bg);
        addView(mInBgCircle, new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        mPressedInBgCircle = new ImageView(getContext());
        mPressedInBgCircle.setImageResource(R.drawable.shape_recorder_in_bg_pressed);
        mPressedInBgCircle.setVisibility(GONE);
        addView(mPressedInBgCircle, new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleProgressbar, 0 , 0);
        int radius = (int) Math.ceil(typeArray.getDimension(R.styleable.CircleProgressbar_radius, 80));
        int strokeWidth = (int) Math.ceil(typeArray.getDimension(R.styleable.CircleProgressbar_strokeWidth, 10));
        mProgressCircleView = new ProgressCircleView(context, attrs);
        mProgressCircleView.setVisibility(GONE);
        addView(mProgressCircleView, new LayoutParams(
                (radius+strokeWidth)*2,
                (radius+strokeWidth)*2, Gravity.CENTER));
    }

    public ProgressCircleView getProgressCircleView() {
        return mProgressCircleView;
    }

    class ProgressCircleView extends View {
        // 画圆环的画笔
        private Paint ringPaint;
        // 圆环颜色
        private int ringColor;
        // 半径
        private float radius;
        // 圆环宽度
        private float strokeWidth;

        public ProgressCircleView(Context context, AttributeSet attrs) {
            super(context, attrs);
            initAttrs(context, attrs);
            initVariable();
        }

        private void initAttrs(Context context, AttributeSet attrs) {
            TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CircleProgressbar, 0 , 0);
            radius = typeArray.getDimension(R.styleable.CircleProgressbar_radius, 80);
            strokeWidth = typeArray.getDimension(R.styleable.CircleProgressbar_strokeWidth, 10);
            ringColor = typeArray.getColor(R.styleable.CircleProgressbar_ringColor, 0xFF0000);
        }

        private void initVariable() {
            ringPaint = new Paint();
            ringPaint.setAntiAlias(true);
            ringPaint.setDither(true);
            ringPaint.setColor(ringColor);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeCap(Paint.Cap.ROUND);
            ringPaint.setStrokeWidth(strokeWidth);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if(mIsRecording) {
                RectF oval = new RectF(strokeWidth/2, strokeWidth/2, getWidth() - strokeWidth/2, getHeight() - strokeWidth/2);
                canvas.drawArc(oval, 0, 0, false, ringPaint);
                canvas.drawArc(oval, -90, ((float) (System.currentTimeMillis() - mStartTime) / mTotalTime) * 360, false, ringPaint);
                invalidate();
            }
        }
    }
}
