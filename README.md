# Video

[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

> Android视频录制，自定义视频大小，支持触摸对焦、两指放大缩小、切换摄像头. 采用FFmpeg对进行视频帧剪裁旋转切割，包体积4M


## 1 前言

[版本更新记录](https://github.com/tsy12321/VideoAndroid/releases)

参考开源项目：

[javacv - 使用opencv+ffmpeg，功能最完整的项目，可以用于拓展参考](https://github.com/bytedeco/javacv)

[RecordVideoDemo - 参照javacv中的Demo使用camera+ffmpeg实现，本项目基于该开源项目开发](https://github.com/szitguy/RecordVideoDemo)

[VideoRecorder - 也是一个不错的录制视频项目，不过so库已经太旧了，sdk超过22无法通过](https://github.com/qdrzwd/VideoRecorder)

## 2 接入SDK

将video的aar包放到项目中的libs下，然后在gradle中添加

```
android {
    ...
}

repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    ...

    compile(name:'video-1.0.0', ext:'aar')
}

```

## 3 使用SDK

具体代码见app中的RecorderActivity

### 3.1 初始化

```
//初始化摄像头
mCamera = CameraHelper.getCamera(this, mCameraFace);       //默认打开后置摄像头

//初始化摄像预览界面
mCameraPreviewView = (CameraPreviewView) findViewById(R.id.camera_preview);
mCameraPreviewView.init(mCamera, RECORDER_WIDTH, RECORDER_HEIGHT);

//初始化recorder
mRecorder = new Recorder.Builder()
        .context(getApplicationContext())
        .camera(mCamera)
        .cameraFace(mCameraFace)
        .outputSize(RECORDER_WIDTH, RECORDER_HEIGHT)
        .outputFilePath(path)
        .build();
```

### 3.2 开始录像

```
mRecorder.start();
```

### 3.3 暂停录像

```
mRecorder.stop();
```

### 3.4 切换摄像头

```
if(mCameraFace == Camera.CameraInfo.CAMERA_FACING_BACK) {
    mCameraFace = Camera.CameraInfo.CAMERA_FACING_FRONT;
} else {
    mCameraFace = Camera.CameraInfo.CAMERA_FACING_BACK;
}

mCamera = CameraHelper.getCamera(this, mCameraFace);
mCameraPreviewView.switchCamera(mCamera);
mRecorder.switchCamera(mCamera, mCameraFace);
```

### 3.5 获取当前录制状态

```
mRecorder.isRecording()
```

### 3.6 获取当前录制时间

```
mRecorder.getRecordingTime()
```

## 4 预览

![](https://github.com/tsy12321/VideoAndroid/blob/master/preview/1.jpg)
![](https://github.com/tsy12321/VideoAndroid/blob/master/preview/2.jpg)
![](https://github.com/tsy12321/VideoAndroid/blob/master/preview/3.jpg)


