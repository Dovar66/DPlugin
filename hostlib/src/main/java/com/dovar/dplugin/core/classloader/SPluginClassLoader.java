package com.dovar.dplugin.core.classloader;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import dalvik.system.DexClassLoader;

/**
 * 用于加载独立插件中dex的classLoader
 * <p>
 * 在加载类时，直接从自己的dex里找，当自己这里没找到又明确不是自己的类时，再去宿主里找。
 */
public class SPluginClassLoader extends DexClassLoader {
    private final ClassLoader host;//宿主的ClassLoader，不允许为空
    private final String pluginPkgName;
    private final String whiteList;//白名单的类直接去宿主找

    public SPluginClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, @NonNull ClassLoader host, ClassLoader parent, String pluginPkgName, String whiteList) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
        this.host = host;
        this.pluginPkgName = pluginPkgName;
        this.whiteList = whiteList;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (inWhiteList(name)) {
            //如果在白名单，就直接去宿主找
            return host.loadClass(name);
        }
        return super.loadClass(name, resolve);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            if (!isOurClass(name)) {
                //自己这里没找到，又明确不是自己的类时，再去宿主找找
                return host.loadClass(name);
            }
            throw e;
        }
    }

    private boolean isOurClass(String name) {
        if (TextUtils.isEmpty(pluginPkgName) || TextUtils.isEmpty(name)) return false;

        String[] pkgsplit = pluginPkgName.split(",");
        for (String pkg : pkgsplit
        ) {
            //如果是某个插件的类,则交给插件ClassLoader去加载
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }

    private boolean inWhiteList(String name) {
        if (TextUtils.isEmpty(whiteList) || TextUtils.isEmpty(name)) return false;

        String[] pkgsplit = whiteList.split(",");
        for (String pkg : pkgsplit
        ) {
            if (name.startsWith(pkg)) {
                return true;
            }
        }
        return false;
    }
}
