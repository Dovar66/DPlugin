package com.dovar.dplugin.core;

import android.content.res.Resources;

import com.dovar.dplugin.core.entity.PluginInfo;

/**
 * Created by weizonghe on 2020/4/24.
 * 已安装的插件信息
 */
public class PluginParts {
    public ClassLoader classLoader;
    public Resources resources;
    public String pkgs;//插件中类的包名，多个包名用逗号分隔
    public PluginInfo pluginInfo;//插件详细信息
    public int mode;//0.独立插件 1.耦合插件

    public static final int MODE_INDEPENDENT = 0;//独立插件
    public static final int MODE_COMBINE = 1;//耦合插件
}
