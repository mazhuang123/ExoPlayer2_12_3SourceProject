package com.migu.player;

/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/8/5 10:41
 * @Description 文件描述：
 */

public interface DataSynthesizer {

    void init();

    void startAudioBean(AudioBean audioBean, MySynthesizerListener mySynthesizerListener);

    void stopSynthesizer();

    void updateSpeaker(String name);

    void updateSpeed(String speed);
}
