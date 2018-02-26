package com.tsy.videodemo;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import android.widget.VideoView;

import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.tsy.sdk.myokhttp.MyOkHttp;
import com.tsy.sdk.myokhttp.response.DownloadResponseHandler;

import java.io.File;
import java.util.List;

/**
 * 视频播放
 */
public class RecoderPlayerActivity extends BaseActivity {

    private static final String INTENT_VIDEO_PATH = "video_path";

    private String mVideoPath;

    private VideoView mVideoView;
    private CircleProgressBar mCircleProgressBar;

    private MyOkHttp mMyOkHttp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recoder_player);

        mMyOkHttp = new MyOkHttp();

        mVideoPath = getIntent().getStringExtra(INTENT_VIDEO_PATH);

        //video view
        mVideoView = (VideoView) findViewById(R.id.video_view);
        mVideoView.setVisibility(View.GONE);

        //loading circle
        mCircleProgressBar = (CircleProgressBar) findViewById(R.id.circleProgressBar);
        mCircleProgressBar.setVisibility(View.GONE);
        mCircleProgressBar.setColorSchemeResources(android.R.color.holo_blue_light);
    }

    @Override
    protected void onStart() {
        super.onStart();

        //申请权限
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        performCodeWithPermission("需要读取本地文件权限", 1, perms, new BaseActivity.PermissionCallback() {
            @Override
            public void hasPermission(List<String> allPerms) {
                if(mVideoPath.contains("http")) {       //网络视频
                    String localFilename = MD5Utils.getMd5(mVideoPath);
                    final String path = Environment.getExternalStorageDirectory() + "/a.com.tsy.videodemo/" + localFilename + ".mp4";  //存储路径
                    String tempPath = Environment.getExternalStorageDirectory() + "/a.com.tsy.videodemo/" + localFilename + "_temp.mp4";  //存储下载路径

                    if (new File(path).exists()) {          //有缓存
                        mVideoPath = path;
                        playLocalVideo();
                    } else {
                        mMyOkHttp.download()
                                .url(mVideoPath)
                                .filePath(tempPath)
                                .tag(this)
                                .enqueue(new DownloadResponseHandler() {
                                    @Override
                                    public void onFinish(File downloadFile) {
                                        downloadFile.renameTo(new File(path));
                                        mVideoPath = path;
                                        playLocalVideo();
                                    }

                                    @Override
                                    public void onProgress(long currentBytes, long totalBytes) {
                                        mCircleProgressBar.setProgress((int) ((float)currentBytes * 100 / totalBytes));
                                    }

                                    @Override
                                    public void onStart(long totalBytes) {
                                        mCircleProgressBar.setVisibility(View.VISIBLE);
                                    }

                                    @Override
                                    public void onFailure(String error_msg) {
                                        Toast.makeText(getApplicationContext(), "视频下载错误", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                } else {        //本地视频
                    playLocalVideo();
                }
            }

            @Override
            public void noPermission(List<String> deniedPerms, List<String> grantedPerms, Boolean hasPermanentlyDenied) {
                if(hasPermanentlyDenied) {
                    alertAppSetPermission("前往设置开启权限");
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mVideoView.pause();
    }

    //播放本地视频
    private void playLocalVideo() {
        //获取屏幕宽度
        DisplayMetrics dm = getResources().getDisplayMetrics();

        //获取视频宽高
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(mVideoPath);
        float mediaWidth = dm.widthPixels;
        float mediaHeight = dm.heightPixels;
        try {
            int rotation = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));

            if(rotation == 90 || rotation == 270) {
                mediaWidth = Float.parseFloat(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
                mediaHeight = Float.parseFloat(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            } else {
                mediaWidth = Float.parseFloat(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
                mediaHeight = Float.parseFloat(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            }
        } catch (NullPointerException e) {
            Toast.makeText(getApplicationContext(), "视频播放错误，请重试", Toast.LENGTH_SHORT).show();
            File file = new File(mVideoPath);
            if (file.exists() && file.isFile()) {
                file.delete();
            }
            finish();
        }

        //根据比例占全屏
        int width = 0;
        int height = 0;
        if(mediaWidth/mediaHeight > dm.widthPixels/dm.heightPixels) {
            width = dm.widthPixels;
            height = (int) (mediaHeight / (mediaWidth/dm.widthPixels));
        } else {
            height = dm.heightPixels;
            width = (int) (mediaWidth / (mediaHeight/dm.heightPixels));
        }
        ViewGroup.LayoutParams layoutParams = mVideoView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        mVideoView.setLayoutParams(layoutParams);

        //视频路径
        mVideoView.setVideoPath(mVideoPath);

        //点击退出
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        finish();
                        break;

                }
                return true;
            }
        });

        //循环播放
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVideoView.seekTo(0);
                mVideoView.start();
            }
        });

        mVideoView.setVisibility(View.VISIBLE);
        mVideoView.start();
    }

    /**
     * 打开视频
     * @param context
     * @param videoPath
     * @return
     */
    public static Intent createIntent(Context context, String videoPath) {
        Intent intent = new Intent(context, RecoderPlayerActivity.class);
        intent.putExtra(INTENT_VIDEO_PATH, videoPath);
        return intent;
    }
}
