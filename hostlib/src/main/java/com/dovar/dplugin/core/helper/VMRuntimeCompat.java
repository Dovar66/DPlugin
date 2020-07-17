package com.dovar.dplugin.core.helper;

import android.os.Build;

import java.lang.reflect.Method;

/**
 * 通过反射 VMRuntime.is64Bit() 获取是否为64位
 */
public class VMRuntimeCompat {

    private static final byte[] GET_LOCKER = new byte[0];

    private static volatile Boolean sIs64Bit;

    /**
     * 精确判断是否为64位
     */
    public static boolean is64Bit() {
        // 最终使用下列方法：
        // VMRuntime.getRuntime().is64Bit();
        if (sIs64Bit != null) {
            return sIs64Bit;
        }
        synchronized (GET_LOCKER) {
            if (sIs64Bit != null) {
                return sIs64Bit;
            }

            // 确保只获取一次。但不排除个别手机一上来获取会有问题（没遇到）
            sIs64Bit = is64BitImpl();
            return sIs64Bit;
        }
    }

    private static boolean is64BitImpl() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // Android API 21之前不支持64位CPU
                return false;
            }

            Class<?> clzVMRuntime = Class.forName("dalvik.system.VMRuntime");
            if (clzVMRuntime == null) {
                return false;
            }
            Method mthVMRuntimeGet = clzVMRuntime.getDeclaredMethod("getRuntime");
            if (mthVMRuntimeGet == null) {
                return false;
            }
            Object objVMRuntime = mthVMRuntimeGet.invoke(null);
            if (objVMRuntime == null) {
                return false;
            }
            Method sVMRuntimeIs64BitMethod = clzVMRuntime.getDeclaredMethod("is64Bit");
            if (sVMRuntimeIs64BitMethod == null) {
                return false;
            }
            Object objIs64Bit = sVMRuntimeIs64BitMethod.invoke(objVMRuntime);
            if (objIs64Bit instanceof Boolean) {
                return (boolean) objIs64Bit;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Art虚拟机，引入AOT编译后，读取oat目录下当前正在使用的目录
     * TODO 目前仅支持arm
     *
     * @return
     */
    public static String getArtOatCpuType() {
        return VMRuntimeCompat.is64Bit() ? BuildCompat.ARM64 : BuildCompat.ARM;
    }
}