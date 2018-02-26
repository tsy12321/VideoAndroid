package com.ci123.sdk.video;

import android.content.Context;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;

import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.bytedeco.javacv.FrameRecorder;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

/**
 * Recorder
 * Created by tsy on 2016/12/29.
 */

public class Recorder implements Camera.PreviewCallback {

    private String TAG = "Recorder";
    private Builder mBuilder;       //参数Builder

    private Frame mFrame;

    private FFmpegFrameRecorder mFFmpegFrameRecorder;

    private FFmpegFrameRecorder mFFmpegFrameRecorderL;      //水平方向
    private FFmpegFrameRecorder mFFmpegFrameRecorderV;      //垂直方向

    private FFmpegFrameFilter mFFmpegFrameFilter;
    private AudioRecordRunnable mAudioRecordRunnable;
    private Thread mAudioThread;
    private AudioRecord mAudioRecord;

    private long mStartTime = 0L;
    private boolean mRecording = false;     //视频是否正在录制
    private boolean mRunAudioThread = false;        //音频是否正在录制

    private int sampleAudioRateInHz = 44100; //声音采样率
    private int frameRate = 30; // 帧率

    Recorder(Builder builder) {
        mBuilder = builder;

        initRecorder();
    }

    /**
     * 获取视频路径
     * @return
     */
    public String getOutputPath() {
        return mBuilder.mOutputFilePath;
    }

    /**
     * 是否正在录制中
     * @return
     */
    public boolean isRecording() {
        return mRecording;
    }

    /**
     * 获取录制时长 单位ms
     * @return
     */
    public long getRecordingTime() {
        if(mStartTime == 0) {
            return 0;
        }
        return System.currentTimeMillis() - mStartTime;
    }

    /**
     * 切换前后摄像头
     * @param camera
     * @param face
     */
    public void switchCamera(Camera camera, int face) {
        mBuilder.switchCamera(camera, face);
        initFilter();
    }

    //初始化
    private void initRecorder() {
        Camera.Size previewSize = CameraHelper.getFullScreenPreviewSize(mBuilder.mContext, mBuilder.mCamera);
        mFrame = new Frame(previewSize.width, previewSize.height, Frame.DEPTH_UBYTE, 2);

        //提前加载2个framerecorder
        mFFmpegFrameRecorderV = new FFmpegFrameRecorder(mBuilder.mOutputFilePath, mBuilder.mOutputWidth, mBuilder.mOutputHeight, 1);
        initFrameRecorder(mFFmpegFrameRecorderV);
        mFFmpegFrameRecorderL = new FFmpegFrameRecorder(mBuilder.mOutputFilePath, mBuilder.mOutputHeight, mBuilder.mOutputWidth, 1);
        initFrameRecorder(mFFmpegFrameRecorderL);

        //音频
        mAudioRecordRunnable = new AudioRecordRunnable();
    }

    //初始化framerecoder
    private void initFrameRecorder(FFmpegFrameRecorder frameRecorder) {
        frameRecorder.setFormat("mp4");
        frameRecorder.setVideoOption("preset", "ultrafast");        //加速
        frameRecorder.setFrameRate(frameRate);
        frameRecorder.setVideoQuality(0);

        frameRecorder.setAudioChannels(1);
        frameRecorder.setSampleRate(sampleAudioRateInHz);
    }

    //初始化filter
    private void initFilter() {
        Camera.Size previewSize = CameraHelper.getFullScreenPreviewSize(mBuilder.mContext, mBuilder.mCamera);

        //根据定义的视频尺寸和预览的尺寸进行切割、旋转
        Float outputRatio = 1f * mBuilder.mOutputHeight / mBuilder.mOutputWidth;
        int cropWidth = (int) (outputRatio * previewSize.height);
        int cropHeight = previewSize.height;
        int startX = 0;
        int startY = 0;
        int rotation = 90;      //顺时针90度
        boolean hflip = false;
        rotation = (rotation + mBuilder.mMyOrientationDetector.getOrientation()) % 360;

        //如果是前置摄像头 改变旋转方向 修改裁剪位置 水平旋转
        if(mBuilder.mCameraFace == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            startX = previewSize.width - cropWidth;
            rotation = 270;        //270度
            rotation = (rotation - mBuilder.mMyOrientationDetector.getOrientation()) % 360;
            hflip = true;       //自拍水平旋转
        }

        String filters = generateFilters(cropWidth, cropHeight, startX, startY, rotation, hflip);
        mFFmpegFrameFilter = new FFmpegFrameFilter(filters, previewSize.width, previewSize.height);
        mFFmpegFrameFilter.setPixelFormat(org.bytedeco.javacpp.avutil.AV_PIX_FMT_NV21); // default camera format on Android
    }

    /**
     * 开始录制
     * @throws FrameRecorder.Exception
     * @throws FrameFilter.Exception
     */
    public void start() throws FrameRecorder.Exception, FrameFilter.Exception {
        if(mRecording) {
            return;
        }

        int rotation = mBuilder.mMyOrientationDetector.getOrientation();
        if(rotation == 0 || rotation == 180) {
            mFFmpegFrameRecorder = mFFmpegFrameRecorderV;
        } else {
            mFFmpegFrameRecorder = mFFmpegFrameRecorderL;
        }

        initFilter();
        mBuilder.mCamera.setPreviewCallback(this);
        mFFmpegFrameRecorder.start();
        mFFmpegFrameFilter.start();
        mRecording = true;
        mRunAudioThread = true;
        mAudioThread = new Thread(mAudioRecordRunnable);
        mAudioThread.start();
        mStartTime = System.currentTimeMillis();
    }

    /**
     * 暂停视频
     * @throws FrameRecorder.Exception
     * @throws FrameFilter.Exception
     */
    public void stop() throws FrameRecorder.Exception, FrameFilter.Exception {
        if(!mRecording) {
            return;
        }

        mStartTime = 0L;
        mRunAudioThread = false;
        try {
            mAudioThread.join();
        } catch (InterruptedException e) {
            // reset interrupt to be nice
            Thread.currentThread().interrupt();
            return;
        }
        mAudioThread = null;

        if (mFFmpegFrameRecorder != null && mRecording) {
            mRecording = false;
            mFFmpegFrameRecorder.stop();
            mFFmpegFrameRecorder.release();
            mFFmpegFrameFilter.stop();
            mFFmpegFrameFilter.release();
        }
    }

    /**
     * 生成处理配置
     * @param w 裁切宽度
     * @param h 裁切高度
     * @param x 裁切起始x坐标
     * @param y 裁切起始y坐标
     * @param rotation 图像旋转度数
     * @param hflip 是否水平翻转
     * @return 帧图像数据处理参数
     */
    private String generateFilters(int w, int h, int x, int y, int rotation, boolean hflip) {
        String filters = String.format("crop=w=%d:h=%d:x=%d:y=%d", w, h, x, y);

        for(int i = 0; i < rotation/90; i ++) {
            filters += ",transpose=clock";
        }

        if(hflip) {
            filters += ",hflip";
        }

        return filters;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        if (mFrame != null && mRecording) {
            ((ByteBuffer)mFrame.image[0].position(0)).put(data);

            try {
                long t = 1000 * (System.currentTimeMillis() - mStartTime);
                if (t > mFFmpegFrameRecorder.getTimestamp()) {
                    mFFmpegFrameRecorder.setTimestamp(t);
                }

                mFFmpegFrameFilter.push(mFrame);
                Frame frame2;
                while ((frame2 = mFFmpegFrameFilter.pull()) != null) {
                    mFFmpegFrameRecorder.record(frame2);            //录制该图片
                }
            } catch (FFmpegFrameRecorder.Exception | FrameFilter.Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class Builder {
        private Context mContext;
        private Camera mCamera;
        private int mCameraFace;
        private String mOutputFilePath;
        private int mOutputWidth;
        private int mOutputHeight;
        private MyOrientationDetector mMyOrientationDetector;

        public Builder() {
            mContext = null;
            mCamera = null;
            mCameraFace = -1;
            mOutputFilePath = "";
            mOutputWidth = 0;
            mOutputHeight = 0;
        }

        public void switchCamera(Camera camera, int face) {
            mCamera = camera;
            mCameraFace = face;
        }

        public Builder context(@NonNull Context context) {
            mContext = context;
            return this;
        }

        public Builder camera(@NonNull Camera camera) {
            mCamera = camera;
            return this;
        }

        public Builder cameraFace(int face) {
            mCameraFace = face;
            return this;
        }

        public Builder outputFilePath(@NonNull String outputFilePath) {
            mOutputFilePath = outputFilePath;
            return this;
        }

        public Builder outputSize(int width, int height) {
            mOutputWidth = width;
            mOutputHeight = height;
            return this;
        }

        public Builder orientationDetector(@NonNull MyOrientationDetector myOrientationDetector) {
            mMyOrientationDetector = myOrientationDetector;
            return this;
        }

        public Recorder build() throws IllegalArgumentException {
            if(mContext == null) {
                throw new IllegalArgumentException("mContext为空");
            }
            if(mCamera == null) {
                throw new IllegalArgumentException("mCamera为空");
            }
            if(mCameraFace != Camera.CameraInfo.CAMERA_FACING_BACK && mCameraFace != Camera.CameraInfo.CAMERA_FACING_FRONT) {
                throw new IllegalArgumentException("mCameraFace非法");
            }
            checkFilePath(mOutputFilePath);
            if(mOutputWidth < 0 || mOutputHeight < 0) {
                throw new IllegalArgumentException("mOutputWidth mOutputHeight非法");
            }
            if(mMyOrientationDetector == null) {
                throw new IllegalArgumentException("mMyOrientationDetector为空");
            }
            return new Recorder(this);
        }

        //检查filePath有效性
        private void checkFilePath(String filePath) throws IllegalArgumentException {
            if(filePath.length() == 0) {
                throw new IllegalArgumentException("mOutputFilePath为空");
            }

            File file = new File(filePath);
            if(file.exists()) {
                return ;
            }

            if (filePath.endsWith(File.separator)) {
                throw new IllegalArgumentException("文件" + filePath + "格式非法，目标文件不能为目录！");
            }

            //判断目标文件所在的目录是否存在
            if(!file.getParentFile().exists()) {
                if(!file.getParentFile().mkdirs()) {
                    throw new IllegalArgumentException("创建目标文件所在目录失败！");
                }
            }
        }
    }


    //---------------------------------------------
    // audio thread, gets and encodes audio data
    //---------------------------------------------
    private class AudioRecordRunnable implements Runnable {

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            // Audio
            int bufferSize;
            ShortBuffer audioData;
            int bufferReadResult;

            bufferSize = AudioRecord.getMinBufferSize(sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleAudioRateInHz,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

            audioData = ShortBuffer.allocate(bufferSize);

            mAudioRecord.startRecording();

            /* ffmpeg_audio encoding loop */
            while (mRunAudioThread) {
                //获取音频数据
                bufferReadResult = mAudioRecord.read(audioData.array(), 0, audioData.capacity());
                audioData.limit(bufferReadResult);
                if (bufferReadResult > 0) {
                    if(mFFmpegFrameRecorder != null && mRecording) {
                        try {
                            mFFmpegFrameRecorder.recordSamples(audioData);      //写入音频数据
                        } catch (FFmpegFrameRecorder.Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            /* encoding finish, release recorder */
            if (mAudioRecord != null) {
                mAudioRecord.stop();
                mAudioRecord.release();
            }
        }
    }
}
