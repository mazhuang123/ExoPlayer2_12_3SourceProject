//package com.migu.player;
//
//import android.text.TextUtils;
//import android.util.Log;
//
//import com.cmcc.miguhelpersdk.MiguHelperApi;
//import com.cmcc.miguhelpersdk.MiguHelperFactory;
//import com.cmcc.miguhelpersdk.listener.OnlineTtsResultListener;
//import com.cmcc.miguhelpersdk.model.OnlineTtsResult;
//import com.cmcc.miguhelpersdk.model.OnlineTtsSessionParam;
//
///**
// * @Author 作者：mazhuang
// * @Date 创建时间：2021/8/4 11:38
// * @Description 文件描述：
// */
//
//public class HttpRequestBox  {
//    MiguHelperApi miguHelperApi;
//    private String voiceName = "xiaozhou";
//    private String speed = "50";
//    public void init() {
//        MiguHelperFactory.init(MyApplication.context, "4c9fl38v", "fec507fab0cc4536ba5956374cef142e", false);
//        miguHelperApi = MiguHelperFactory.createApi(MyApplication.context);
//    }
//
//    public void start(final String content, final MySynthesizerListener mySynthesizerListener) {
//        String mAudioCoding = "raw";
//        OnlineTtsSessionParam mTtsSessionParams = new OnlineTtsSessionParam();
//        mTtsSessionParams.setNative_voice_name("liyitong2");
//        mTtsSessionParams.setPitch("" + 50);
//        mTtsSessionParams.setVolume("" + 15);
//        mTtsSessionParams.setSpeed(speed);
//        mTtsSessionParams.setSample_rate("16000");
//        mTtsSessionParams.setAudio_coding(mAudioCoding);
//        if (!TextUtils.isEmpty(mAudioCoding) && (mAudioCoding.equals("speex") || mAudioCoding.equals("speex-wb"))) {
//            mTtsSessionParams.setFrame_size("" + 640);
//        }
//        mTtsSessionParams.setRead_english("" + 0);
//        mTtsSessionParams.setRead_number("" + 0);
//        miguHelperApi.getOnlineTTS(content,content, mTtsSessionParams, new OnlineTtsResultListener() {
//            @Override
//            public void onResponse(OnlineTtsResult result) {
//                try {
//                    if (result != null) {
//                        Log.e("Online+++","合成成功的sid为: "+result.getSid());
//                        byte[] bytes = result.getBytes();
//                        if (bytes != null) {
//                            byte[] wavHeader = ByteUtil.writeHeader(bytes.length);
//                            byte[] appendResult = ByteUtil.byteMerger(wavHeader, bytes);
//                            mySynthesizerListener.onRequestSuccess(content,appendResult);
//                        }
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            @Override
//            public void onError(String code, String error) {
//            }
//        });
//    }
//
//}
