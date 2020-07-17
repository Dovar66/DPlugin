package com.dovar.dplugin.plugin;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.dovar.dplugin.core.LoadedPluginManager;
import com.dovar.dplugin.core.PluginParts;

/**
 * 独立插件中所有Service的基类
 */
public class DPluginService extends Service {

    private Resources mixResources;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    protected void attachBaseContext(Context base) {
        PluginParts parts = LoadedPluginManager.instance().lookupPlugin(getClass().getClassLoader());
        if (parts != null && parts.resources != null) {
            if (parts.mode == PluginParts.MODE_INDEPENDENT) {
                //系统会在插件中获取Application的icon，所以还是要访问宿主资源
                Resources host = LoadedPluginManager.instance().getHostResources();
                mixResources = new SPluginResources(host, parts.resources);
            } else {
                //耦合插件不需要继承 DPluginService
                //即使继承了，这里也啥都不干
            }
        }
        super.attachBaseContext(base);
    }

    @Override
    public Resources getResources() {
        if (mixResources != null) {
            return mixResources;
        }
        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if (mixResources != null) {
            return mixResources.getAssets();
        }
        return super.getAssets();
    }
}
