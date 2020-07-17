package com.dovar.dplugin.core.classloader;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dalvik.system.PathClassLoader;

/**
 * 宿主ClassLoader的parent
 * <p>
 * 当加载类时，优先从自己加载，没找到时再根据包名分配到对应的插件中加载
 */
public class ManagerClassLoader extends PathClassLoader {
    private static final ConcurrentHashMap<String, ClassLoader> pluginClassLoaders = new ConcurrentHashMap<>();//pkg <- -> ClassLoader


    public ManagerClassLoader(@NonNull ClassLoader parent) {
        super("", "", parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        try {
            //super.loadClass()调用之前不能出现未找过的类，否则在4.4设备上会挂掉
            return super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            if (!TextUtils.isEmpty(name)) {
                for (Map.Entry<String, ClassLoader> entry : pluginClassLoaders.entrySet()
                ) {
                    //如果是某个插件的类,则交给插件ClassLoader去加载
                    if (name.startsWith(entry.getKey())) {
                        return entry.getValue().loadClass(name);
                    }
                }
            }
            // TODO: 2020/4/27 直接返回null好像也可以，因为宿主的classLoader会捕获这个异常，最后再重新抛一个异常出来
            throw e;//必须将异常抛出去
        }
    }

    public static void cachePluginClassLoader(String pkgs, ClassLoader classLoader) {
        if (TextUtils.isEmpty(pkgs) || classLoader == null) return;
        //提前分好吧
        String[] pkgSplit = pkgs.split(",");
        for (String pkg : pkgSplit
        ) {
            if (TextUtils.isEmpty(pkg)) continue;
            pluginClassLoaders.put(pkg, classLoader);
        }
    }
}
