package com.migu.player;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.iflytek.cloud.BufferCallBackListener;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

import java.util.ArrayList;

/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/8/5 10:31
 * @Description 文件描述：
 */

public class OfflineSynthesizer implements DataSynthesizer {
    private SpeechSynthesizer speechSynthesizer;
    private String voiceName = "xiaofeng";

    public OfflineSynthesizer() {
        init();
    }

    @Override
    public void init(){
        checkSpeechLicence();
        speechSynthesizer = SpeechSynthesizer.createSynthesizer(MyApplication.context, mTtsInitListener);
        setParameter();
    }

    @Override
    public synchronized void startAudioBean(final AudioBean audioBean, final MySynthesizerListener mySynthesizerListener) {
        WebAudioDataManager.getInstance().add(audioBean);
        speechSynthesizer.setBufferCallBackListener(new BufferCallBackListener() {
            @Override
            public void onBufferData(ArrayList<byte[]> arrayList) {
                byte[] synBytes = ByteUtil.transferByte(arrayList);
                AudioBean audioBean1 = WebAudioDataManager.getInstance().queryAudioData(audioBean.getSid());
                audioBean1.setResultBytes(synBytes);
                mySynthesizerListener.onRequestSuccess(audioBean1.getContent(),synBytes);
            }

            @Override
            public void onCompleted(SpeechError speechError) {
                if(speechError != null){
                    Log.e("MSC_LOG","错误码为: "+speechError.getErrorCode());
                }
            }
        });
        speechSynthesizer.startSpeaking(audioBean.getContent(), mTtsListener);
    }

    @Override
    public void stopSynthesizer() {
        speechSynthesizer.stopSpeaking();
    }

    @Override
    public void updateSpeaker(String name) {

    }

    @Override
    public void updateSpeed(String speed) {

    }

    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {
        @Override
        public void onSpeakBegin() {
            LogUtil.logAudioDevice("开始播放");
        }

        @Override
        public void onSpeakPaused() {
            LogUtil.logAudioDevice("暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            LogUtil.logAudioDevice("继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
            LogUtil.logAudioDevice("离线合成进度：" + percent);

        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                LogUtil.logAudioDevice("朗读完成，开始朗读下一段");
            }
        }

        @Override
        public void onEvent(int arg0, int arg1, int arg2, Bundle arg3) {
            // TODO Auto-generated method stub

        }

    };
    private void checkSpeechLicence() {
        StringBuffer param = new StringBuffer();
        param.append("appid=6018be93");
        SpeechUtility.createUtility(MyApplication.context, param.toString());
    }

    //获取本地高品质发音人资源
    private String getHighResourcePath(String voiceName) {
        StringBuffer tempBuffer = new StringBuffer();
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(MyApplication.context,
                ResourceUtil.RESOURCE_TYPE.assets, "xtts/pureXtts_common.jet"));
        tempBuffer.append(";");
        //发音人资源
        tempBuffer.append(ResourceUtil.generateResourcePath(MyApplication.context,
                ResourceUtil.RESOURCE_TYPE.assets, "xtts/pureXtts_" + voiceName + ".jet"));
        return tempBuffer.toString();
    }
    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
            }
        }
    };
    public void setParameter() {
        if (speechSynthesizer == null) {
            return;
        }
        // 清空参数
        speechSynthesizer.setParameter(SpeechConstant.PARAMS, null);
        speechSynthesizer.setParameter(SpeechConstant.SPEED, "120");
        speechSynthesizer.setParameter(SpeechConstant.ENGINE_TYPE, "purextts");
        //设置发音人资源路径
        speechSynthesizer.setParameter(ResourceUtil.TTS_RES_PATH, getHighResourcePath(voiceName));
        //设置发音人
        speechSynthesizer.setParameter(SpeechConstant.VOICE_NAME, voiceName);
        //设置播放器音频流类型
        speechSynthesizer.setParameter(SpeechConstant.STREAM_TYPE, "3");
        //保存合成音频文件
        speechSynthesizer.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        speechSynthesizer.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/wav/test.wav");
//        speechSynthesizer.setParameter(SpeechConstant.NEXT_TEXT,getResources().getString(contentArray[nextIndex]));
    }
}
