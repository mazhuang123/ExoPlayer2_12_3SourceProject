package com.migu.player;


import java.util.ArrayList;

/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/6/10 17:27
 * @Description 文件描述：用于维护合成后的音频数据
 */

public class WebAudioDataManager {
    public ArrayList<AudioBean> audioDataList = new ArrayList<>();
    private WebAudioDataManager(){}
    private static  WebAudioDataManager instance;
    public static WebAudioDataManager getInstance(){
        if(instance == null){
            synchronized (WebAudioDataManager.class){
                if(instance == null){
                    instance = new WebAudioDataManager();
                }
            }
        }
        return instance;
    }

    public synchronized void add(AudioBean audioData){
        if(audioDataList == null){
            audioDataList = new ArrayList<>();
        }
        audioDataList.add(audioData);
    }
    public AudioBean getData(int index){
        if(index>=audioDataList.size()){
            return null;
        }
        return audioDataList.get(index);
    }
    public synchronized void remove(AudioBean audioData){
        if(audioDataList!=null){
            audioDataList.remove(audioData);
        } else {
            audioDataList = new ArrayList<>();
        }
    }

    public synchronized void removeAllData(){
        if(audioDataList!=null){
            audioDataList.clear();
            audioDataList = null;
        }
    };
    public AudioBean queryAudioData(String sid){
        for(AudioBean audioBean : audioDataList){
            if(audioBean.getSid().equals(sid)){
                return audioBean;
            }
        }
        return null;
    }
}
