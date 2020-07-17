package com.dovar.dplugin.core;

import android.content.res.Resources;
import android.text.TextUtils;

import com.dovar.dplugin.core.entity.PluginInfo;

import java.util.HashMap;
import java.util.Map;

public class LoadedPluginManager {
    private Resources hostResources;
    private final HashMap<String, PluginParts> mPluginPartsMap = new HashMap<>();//所有已加载插件


    private static class SingleTon {
        private static final LoadedPluginManager inst = new LoadedPluginManager();
    }

    private LoadedPluginManager() {

    }

    public static LoadedPluginManager instance() {
        return SingleTon.inst;
    }

    public void setHostResources(Resources host) {
        hostResources = host;
    }

    public Resources getHostResources() {
        return hostResources;
    }


    public synchronized boolean installPlugin(String pluginName, String pkgs, ClassLoader cl, Resources pluginRes, PluginInfo info) {
        if (TextUtils.isEmpty(pluginName) || TextUtils.isEmpty(pkgs) || mPluginPartsMap.containsKey(pluginName))
            return false;
        PluginParts parts = new PluginParts();
        parts.pkgs = pkgs;
        parts.classLoader = cl;
        parts.resources = pluginRes;
        parts.pluginInfo = info;
        mPluginPartsMap.put(pluginName, parts);
        return true;
    }

    /**
     * 根据类加载器找到对应的PluginParts
     */
    public synchronized PluginParts lookupPlugin(ClassLoader pluginClassLoader) {
        for (Map.Entry<String, PluginParts> entry : mPluginPartsMap.entrySet()
        ) {
            PluginParts parts = entry.getValue();
            if (parts != null && parts.classLoader == pluginClassLoader) {
                return parts;
            }
        }
        return null;
    }

    /**
     * 根据插件名查找PluginParts
     */
    public synchronized PluginParts lookupPlugin(String pluginName) {
        return mPluginPartsMap.get(pluginName);
    }
}
