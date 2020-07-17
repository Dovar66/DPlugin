package com.dovar.dplugin.core;

import android.content.Context;

import com.dovar.dplugin.core.entity.PluginInfo;


/**
 * 插件化框架对外事件回调接口集
 * <p>
 * 宿主需继承此类，并复写相应的方法来自定义插件框架的事件处理机制
 */
public class DPluginEventCallbacks {

    protected final Context mContext;

    public DPluginEventCallbacks(Context context) {
        mContext = context;
    }

    /**
     * 安装插件失败
     *
     * @param path 插件路径
     * @param code 安装失败的原因
     */
    public void onInstallPluginFailed(String path, InstallResult code) {
        // Nothing
    }

    /**
     * 安装插件成功
     *
     * @param info 插件信息
     */
    public void onInstallPluginSucceed(PluginInfo info) {
        // Nothing

    }

    /**
     * 插件加载失败
     */
    public void onLoadPluginFailed(PluginInfo info) {

    }

    /**
     * 插件加载成功
     */
    public void onLoadPluginSucceed(PluginInfo info) {

    }


    /**
     * 插件安装结果值
     */
    public enum InstallResult {
        READ_PKG_INFO_FAIL,
        VERIFY_SIGN_FAIL,
        VERIFY_VER_FAIL,
        COPY_APK_FAIL,
    }
}