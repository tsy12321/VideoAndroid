# Video

[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Download](https://api.bintray.com/packages/tangsiyuan/maven/video-sdk/images/download.svg) ](https://bintray.com/tangsiyuan/maven/video-sdk/_latestVersion)

> Android视频录制，自定义视频大小，支持触摸对焦、两指放大缩小、切换摄像头. 采用FFmpeg对进行视频帧剪裁旋转切割，包体积4M

## 1 前言

功能简介：
- 视频录制、播放
- 自定义视频大小
- 翻转摄像头、翻转后图像翻转功能
- 触摸对焦、两指放大缩小

[版本更新记录](https://github.com/tsy12321/VideoAndroid/releases)

参考开源项目：

[javacv - 使用opencv+ffmpeg，功能最完整的项目，可以用于拓展参考](https://github.com/bytedeco/javacv)

[RecordVideoDemo - 参照javacv中的Demo使用camera+ffmpeg实现，本项目基于该开源项目开发](https://github.com/szitguy/RecordVideoDemo)

[VideoRecorder - 也是一个不错的录制视频项目，不过so库已经太旧了，sdk超过22无法通过](https://github.com/qdrzwd/VideoRecorder)

## 2 接入SDK

在gradle中添加

```
...
dependencies {
    ...
    compile 'com.tsy.video:video-sdk:1.0.0'
}
...

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


## About Me
简书地址：http://www.jianshu.com/users/21716b19302d/latest_articles

微信公众号

![我的公众号](https://github.com/tsy12321/PayAndroid/blob/master/wxmp_avatar.jpg)

License
-------

    Copyright 2017 SY.Tang

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.