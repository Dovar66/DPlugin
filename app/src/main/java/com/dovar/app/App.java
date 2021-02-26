package com.dovar.app;

import android.app.Application;
import android.content.Context;

import com.dovar.dplugin.core.DPlugin;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        DPlugin.App.attachBaseContext(this);
    }
}
