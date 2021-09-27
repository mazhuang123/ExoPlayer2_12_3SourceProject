package com.migu.player;

import android.app.Application;
import android.content.Context;

/**
 * @Author 作者：mazhuang
 * @Date 创建时间：2021/7/1 15:02
 * @Description 文件描述：
 */

public class MyApplication extends Application {
    public static Context context;
    @Override
    public void onCreate() {
        super.onCreate();
        context = this;
    }
}
