package top.yuxin.hookdemo;

import android.app.Application;

import top.yuxin.hookdemo.hookutils.ClipboadHookDemo;

/**
 * Created by yuxin-hu on 17-11-27.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ClipboadHookDemo.hookService();
    }
}
