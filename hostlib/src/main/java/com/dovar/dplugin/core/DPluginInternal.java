package com.dovar.dplugin.core;

import android.app.Application;

/**
 * 对框架暴露的一些通用的接口。
 * <p>
 * 注意：插件框架内部使用，外界请不要调用。
 */
public class DPluginInternal {

    private static Application sAppContext;

    static void init(Application app) {
        sAppContext = app;
    }

    /**
     * 获取宿主注册时的Context对象
     */
    public static Application getAppContext() {
        return sAppContext;
    }

    /**
     * 获取宿主注册时的ClassLoader
     */
    public static ClassLoader getAppClassLoader() {
        return getAppContext().getClassLoader();
    }

    /**
     * 获取{@link com.dovar.dplugin.core.classloader.ManagerClassLoader}
     */
    public static ClassLoader getManagerClassLoader() {
        return getAppContext().getClassLoader().getParent();
    }
}
