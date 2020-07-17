package com.dovar.dplugin.core.entity;

public class Constant {

    /**
     * 插件存放目录
     */
    public static final String LOCAL_PLUGIN_APK_SUB_DIR = "p_apk";

    /**
     * 释放Odex的目录
     */
    public static final String LOCAL_PLUGIN_APK_ODEX_SUB_DIR = "p_odex";

    /**
     * 插件的Native（SO库）存放目录
     */
    public static final String LOCAL_PLUGIN_APK_LIB_DIR = "p_so";

    /**
     * 插件extra dex（优化前）释放的以插件名独立隔离的子目录
     * 适用于 android 5.0 以下，5.0以上不会用到该目录
     */
    public static final String LOCAL_PLUGIN_INDEPENDENT_EXTRA_DEX_SUB_DIR = "_ed";

    /**
     * 插件extra dex（优化后）释放的以插件名独立隔离的子目录
     */
    public static final String LOCAL_PLUGIN_INDEPENDENT_EXTRA_ODEX_SUB_DIR = "_eod";

    /**
     * 插件加载时的进程锁文件,插件间不共用一把锁
     */
    public static final String LOAD_PLUGIN_LOCK = "plugin_%s.lock";
}
