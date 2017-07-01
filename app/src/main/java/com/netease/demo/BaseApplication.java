package com.netease.demo;

import android.app.Application;

/**
 * Created by bjwangmingxian on 17/6/22.
 */

public class BaseApplication extends Application {

    public static long sUiThreadId;

    @Override
    public void onCreate() {
        super.onCreate();
        sUiThreadId = Thread.currentThread().getId();
    }

}
