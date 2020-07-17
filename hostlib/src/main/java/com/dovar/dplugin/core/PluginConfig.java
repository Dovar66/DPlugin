package com.dovar.dplugin.core;

import android.content.Context;

import com.dovar.dplugin.core.util.PluginLogUtil;


public final class PluginConfig {

    private DPluginEventCallbacks eventCallbacks;

    private boolean optimizeArtLoadDex = false;

    /**
     * 获取插件化框架的事件回调方法，通常无需调用此方法。
     *
     * @return 可供外界使用的回调
     */
    public DPluginEventCallbacks getEventCallbacks() {
        return eventCallbacks;
    }

    /**
     * 设置插件化框架的事件回调方法，调用者可自定义插件框架的事件回调行为
     *
     * @param eventCallbacks 可供外界使用的回调
     */
    public PluginConfig setEventCallbacks(DPluginEventCallbacks eventCallbacks) {
        if (!checkAllowModify()) {
            return this;
        }
        this.eventCallbacks = eventCallbacks;
        return this;
    }

    void initDefaults(Context context) {
        if (eventCallbacks == null) {
            eventCallbacks = new DPluginEventCallbacks(context);
        }
    }

    // 不允许在attachBaseContext调用完成之后再来修改PluginConfig对象中的内容
    private boolean checkAllowModify() {
        if (DPlugin.App.sAttached) {
            // 不能在此处抛异常，因为个别情况下，宿主的attachBaseContext可能会被调用多次，导致最终出现异常。这里只打出日志即可。
            // throw new IllegalStateException("Already called attachBaseContext. Do not modify!");
            PluginLogUtil.e("rpc.cam: do not modify");
            return false;
        }
        return true;
    }

    /**
     * 是否在Art上对首次加载插件速度做优化
     *
     * @return
     */
    public boolean isOptimizeArtLoadDex() {
        return optimizeArtLoadDex;
    }

    /**
     * 是否在Art上对首次加载插件速度做优化，默认为false
     *
     * @param optimizeArtLoadDex
     * @return
     * @since 2.2.2
     */
    public PluginConfig setOptimizeArtLoadDex(boolean optimizeArtLoadDex) {
        if (!checkAllowModify()) {
            return this;
        }
        this.optimizeArtLoadDex = optimizeArtLoadDex;
        return this;
    }
}