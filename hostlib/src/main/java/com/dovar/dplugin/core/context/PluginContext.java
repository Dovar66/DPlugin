package com.dovar.dplugin.core.context;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.ContextThemeWrapper;

/**
 * Created by weizonghe on 2020/5/11.
 * <p>
 * 作用：让插件Activity/Service的mResources始终为系统API生成的Resources
 */
public class PluginContext extends ContextThemeWrapper {
    private Resources pluginRes;

    public PluginContext(Context base, int themeRes, Resources.Theme mTheme, Resources pluginResources) {
        super(base, themeRes);
        this.pluginRes = pluginResources;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //是否必要？或许被启动的Activity/service并没有用到mBase的theme
            setTheme(mTheme);
        }
    }

    @Override
    public Resources getResources() {
        if (pluginRes != null) return pluginRes;
        return super.getResources();
    }
}
