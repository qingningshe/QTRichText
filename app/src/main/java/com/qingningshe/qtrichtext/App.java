package com.qingningshe.qtrichtext;

import android.app.Application;

/**
 * @author wanglei
 * @version 1.0.0
 * @description App
 * @createTime 2015/10/19
 * @editTime
 * @editor
 */
public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        UILKit.init(getApplicationContext());        //初始化UIL
    }

}
