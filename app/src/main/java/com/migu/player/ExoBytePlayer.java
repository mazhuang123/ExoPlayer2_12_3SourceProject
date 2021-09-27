package com.migu.player;

import android.net.Uri;
import android.util.Log;


import com.migu.player.extractor.DefaultExtractorsFactory;
import com.migu.player.source.ExtractorMediaSource;
import com.migu.player.source.MediaSource;
import com.migu.player.source.TrackGroupArray;
import com.migu.player.trackselection.TrackSelectionArray;
import com.migu.player.upstream.ByteArrayDataSource;
import com.migu.player.upstream.DataSource;

import java.util.ArrayList;


/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/6/10 10:58
 * @Description 文件描述： 直接播放byte[]数据，无需存入本地再播放  接口返回的是PCM数据，需要加上wav头
 *
 * ExoPlayer没播放完一度音频数据,不需要重新创建新的Exoplayer对象即可实现连续播放  注意和SessionPlayer的区别
 *
 */

public class ExoBytePlayer implements Player.EventListener {
    private SimpleExoPlayer simpleExoPlayer;
    public ExoBytePlayer() {
        simpleExoPlayer = new SimpleExoPlayer.Builder(MyApplication.context).build();
        simpleExoPlayer.addListener(this);
    }

    private void prepareDataSource(byte[] data) {
        final ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(data);
        DataSource.Factory factory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return byteArrayDataSource;
            }
        };
        MediaSource mediaSource = new ExtractorMediaSource.Factory(factory)
                .setExtractorsFactory(new DefaultExtractorsFactory())
                .createMediaSource(Uri.EMPTY);

        simpleExoPlayer.prepare(mediaSource);
    }

    public void release() {
        simpleExoPlayer.stop(true);
    }

    /**
     * 通过转化为uri的方式
     *
     */
//    public void playWithUri(byte[] data) {
//        final ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(data);
//        Uri audioByteUri = new ExoPlayerUriHelper().getUri(data);
//        DataSpec dataSpec = new DataSpec(audioByteUri);
//        try {
//            byteArrayDataSource.open(dataSpec);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//
//        DataSource.Factory factory = new DataSource.Factory() {
//            @Override
//            public DataSource createDataSource() {
//                return byteArrayDataSource;
//            }
//        };
//        MediaSource audioSource = new ExtractorMediaSource(audioByteUri,
//                factory, new DefaultExtractorsFactory(),null,null);
//        simpleExoPlayer.prepare(audioSource);
//    }
    public void playStart(byte[] data) {
        Log.e("play+++","开始播放>>>>>>>>>>>>>");
        prepareDataSource(data);
        if (simpleExoPlayer != null) {
            try {
                simpleExoPlayer.play();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void playStartArrayList(ArrayList<byte[]> data) {

    }

    public void updateSpeed(int speed) {
        if (simpleExoPlayer == null) {
            return;
        }
        // 映射速率
        float realSpeed = getRealSpeed(speed);
        // API 23 （6.0）以上 ，通过设置Speed改变音乐的播放速率
        if (isDataPlaying()) {
            // 判断是否正在播放，未播放时，要在设置Speed后，暂停音乐播放
            simpleExoPlayer.setPlaybackParameters(new PlaybackParameters(realSpeed));
        } else {
            simpleExoPlayer.setPlaybackParameters(new PlaybackParameters(realSpeed));
            simpleExoPlayer.setPlayWhenReady(false);
        }
    }


    public boolean isDataPlaying() {
        return simpleExoPlayer != null && simpleExoPlayer.isPlaying();
    }
    public void playPause() {
        if (simpleExoPlayer == null) {
            return;
        }
        simpleExoPlayer.pause();
    }

    public void playResume() {
        if (simpleExoPlayer == null) {
            return;
        }
        simpleExoPlayer.play();
    }

    public void playStop() {
        if (simpleExoPlayer == null) {
            return;
        }
        simpleExoPlayer.stop();
    }


    //    public void playChangeSpeed(int speed) {
//        if (simpleExoPlayer == null) {
//            return;
//        }
//        // 映射速率
//        float realSpeed = getRealSpeed(speed);
//        // API 23 （6.0）以上 ，通过设置Speed改变音乐的播放速率
//        if (isDataPlaying()) {
//            // 判断是否正在播放，未播放时，要在设置Speed后，暂停音乐播放
//            simpleExoPlayer.setPlaybackParameters(new PlaybackParameters(realSpeed));
//        } else {
//            simpleExoPlayer.setPlaybackParameters(new PlaybackParameters(realSpeed));
//            simpleExoPlayer.setPlayWhenReady(false);
//        }
//    }
    private float getRealSpeed(int speed) {
        return 0.7f + 0.026f * speed;
    }


    @Override
    public void onIsLoadingChanged(boolean b) {
        Log.e("play+++","onIsLoadingChanged");
    }

    @Override
    public void onIsPlayingChanged(boolean b) {
        Log.e("play+++","onIsPlayingChanged");
    }

    @Override
    public void onLoadingChanged(boolean b) {
        Log.e("play+++","onLoadingChanged");
    }

    @Override
    public void onMediaItemTransition(MediaItem mediaItem, int i) {
        Log.e("play+++","onMediaItemTransition");
    }

    @Override
    public void onPlayWhenReadyChanged(boolean b, int i) {
        Log.e("play+++","onPlayWhenReadyChanged");
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {
        Log.e("play+++","onPlaybackParametersChanged");
    }

    @Override
    public void onPlaybackStateChanged(int state) {
        Log.e("play+++","onPlaybackStateChanged");
        if (state == Player.STATE_ENDED) {
        }
    }

    @Override
    public void onPlaybackSuppressionReasonChanged(int i) {
        Log.e("play+++","onPlaybackSuppressionReasonChanged");
    }

    @Override
    public void onPlayerError(ExoPlaybackException e) {
        Log.e("play+++","onPlayerError");
    }

    @Override
    public void onPlayerStateChanged(boolean b, int i) {
        Log.e("play+++","onPlayerStateChanged");
    }

    @Override
    public void onPositionDiscontinuity(int i) {
        Log.e("play+++","onPositionDiscontinuity");
    }

    @Override
    public void onRepeatModeChanged(int i) {
        Log.e("play+++","onRepeatModeChanged");
    }

    @Override
    public void onSeekProcessed() {
        Log.e("play+++","onSeekProcessed");
    }

    @Override
    public void onShuffleModeEnabledChanged(boolean b) {
        Log.e("play+++","onShuffleModeEnabledChanged");
    }

    @Override
    public void onTimelineChanged(Timeline timeline, int i) {
        Log.e("play+++","onTimelineChanged");
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object o, int i) {
        Log.e("play+++","onTimelineChanged");
    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
        Log.e("play+++","onTracksChanged");
    }

}
