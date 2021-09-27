package com.migu.player;

import android.net.Uri;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;


import com.migu.player.extractor.DefaultExtractorsFactory;
import com.migu.player.source.ConcatenatingMediaSource;
import com.migu.player.source.ExtractorMediaSource;
import com.migu.player.source.MaskingMediaSource;
import com.migu.player.source.MediaSource;
import com.migu.player.source.TrackGroupArray;
import com.migu.player.trackselection.TrackSelectionArray;
import com.migu.player.upstream.ByteArrayDataSource;
import com.migu.player.upstream.DataSource;

import java.util.HashMap;
import java.util.Map;



/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/6/10 10:58
 * @Description 文件描述： 直接播放byte[]数据，无需存入本地再播放  接口返回的是PCM数据，需要加上wav头
 *
 * ExoPlayer没播放完一度音频数据,不需要重新创建新的Exoplayer对象即可实现连续播放  注意和SessionPlayer的区别
 *
 */

public class ExoByteListPlayer implements Player.EventListener {
    private SimpleExoPlayer simpleExoPlayer;
    private Map<String,String> showMap = new HashMap<>();
    private Map<String, MediaSource> playMap = new HashMap<>();
    private Handler handler = new Handler();
    ConcatenatingMediaSource mediaSourceList ;
    public ExoByteListPlayer() {
        simpleExoPlayer = new SimpleExoPlayer.Builder(MyApplication.context).build();
        simpleExoPlayer.addListener(this);
        mediaSourceList = new ConcatenatingMediaSource();
    }
    public void prepareDataSource(byte[] data,String content){
        MediaSource mediaSource = createMediaSource(data);
        Log.e("id+++","创建的id为: "+ mediaSource.toString());
        playMap.put(mediaSource.toString(),mediaSource);
        showMap.put(mediaSource.toString(),content);
        mediaSourceList.addMediaSource(mediaSource);
    }
    public void justPlay(){
        simpleExoPlayer.prepare(mediaSourceList);
        simpleExoPlayer.play();
    }
    private MediaSource createMediaSource(byte[] data) {
        final ByteArrayDataSource byteArrayDataSource = new ByteArrayDataSource(data);
        DataSource.Factory dataSourceFactory = new DataSource.Factory() {
            @Override
            public DataSource createDataSource() {
                return byteArrayDataSource;
            }
        };
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .setExtractorsFactory(new DefaultExtractorsFactory())
                .createMediaSource(Uri.EMPTY);
        return mediaSource;
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
        Log.e("play+++","onMediaItemTransition,");
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
        } else if (state == Player.STATE_READY) {
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
        Log.e("order+++","onTracksChanged "+simpleExoPlayer.getCurrentWindowIndex());
        MaskingMediaSource mediaSource = (MaskingMediaSource) mediaSourceList.getMediaSource(simpleExoPlayer.getCurrentWindowIndex());
        String id = mediaSource.getMediaSourceId();
        Log.e("id+++","获取到的地址值为: "+id);
    }

}
