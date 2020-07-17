package com.dovar.dplugin.core.classloader;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import dalvik.system.DexClassLoader;

/**
 * 用于加载耦合插件中dex的classLoader
 * <p>
 * 在加载类时，匹配到插件包名才从自己的dex里找，否则去宿主里找。
 */
public class PluginClassLoader extends DexClassLoader {
    private final ClassLoader host;//宿主的ClassLoader，不允许为空
    private final String pluginPkgName;

    public PluginClassLoader(String dexPath, String optimizedDirectory, String librarySearchPath, @NonNull ClassLoader host, ClassLoader parent, String pluginPkgName) {
        super(dexPath, optimizedDirectory, librarySearchPath, parent);
        this.host = host;
        this.pluginPkgName = pluginPkgName;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (isOurClass(name) || isCoreClassInPlugin(name)) {
            //明确是自己的类，则直接加载
            return super.loadClass(name, resolve);
        } else {
            //否则先尝试去宿主加载
            try {
                return host.loadClass(name);
            } catch (ClassNotFoundException e) {
                //宿主没找到，再从插件加载
                //这种情况一般是，宿主混淆时剔除掉了公共库的某些类，但插件却用到了这些类
                return super.loadClass(name, resolve);
            }
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

    /**
     * 正常情况下，耦合插件不会用到pluginlib里的类
     * 但假如还是有人用了，那也兼容下，这些类只会被打包在插件里
     */
    private boolean isCoreClassInPlugin(String name) {
        if (TextUtils.isEmpty(name)) return false;
        return name.startsWith("com.dovar.dplugin.plugin");
    }
}
