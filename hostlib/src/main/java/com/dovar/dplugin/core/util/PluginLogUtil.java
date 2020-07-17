package com.dovar.dplugin.core.util;

import android.text.TextUtils;
import android.util.Log;

public class PluginLogUtil {
    private static boolean isDebug = false;
    private static final String TAG = "DPlugin";

    public static void enable(boolean enable) {
        isDebug = enable;
    }

    public static void d(String tag, String msg) {
        if (!isDebug) return;
        if (TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)) return;
        Log.d(tag, msg);
    }

    public static void d(String msg) {
        d(TAG, msg);
    }

    public static void e(String tag, String msg) {
        if (!isDebug) return;
        if (TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)) return;
        Log.e(tag, msg);
    }

    public static void e(String msg) {
        e(TAG, msg);
    }

    public static void i(String tag, String msg) {
        if (!isDebug) return;
        if (TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)) return;
        Log.i(tag, msg);
    }

    public static void i(String msg) {
        i(TAG, msg);
    }

    public static void w(String tag, String msg) {
        if (!isDebug) return;
        if (TextUtils.isEmpty(tag) || TextUtils.isEmpty(msg)) return;
        Log.w(tag, msg);
    }

    public static void w(String msg) {
        w(TAG, msg);
    }
}
