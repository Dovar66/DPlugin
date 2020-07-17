package com.dovar.dplugin.core;

import android.app.Application;
import android.text.TextUtils;

import com.dovar.dplugin.core.classloader.HookClassLoaderUtils;
import com.dovar.dplugin.core.entity.PluginInfo;
import com.dovar.dplugin.core.loader.PluginLoader;
import com.dovar.dplugin.core.util.PluginLogUtil;

import java.io.File;

public class DPlugin {
    private static final String TAG = "DPlugin";

    /**
     * 安装或升级此插件 <p>
     * 注意： <p>
     *
     * @param path 插件安装的地址。必须是“绝对路径”。通常可以用context.getFilesDir()来做
     * @return 安装成功的插件信息，外界可直接读取
     */
    public static boolean install(String pluginName, String path) {
        if (TextUtils.isEmpty(pluginName)) {
            PluginLogUtil.e(TAG, "install: pluginName is empty!");
            return false;
        }
        if (TextUtils.isEmpty(path)) {
            PluginLogUtil.e(TAG, "install: path is empty!");
            return false;
        }
        // 判断文件合法性
        File file = new File(path);
        if (!file.exists()) {
            PluginLogUtil.e(TAG, "install: File not exists. path=" + path);
            return false;
        } else if (!file.isFile()) {
            PluginLogUtil.e(TAG, "install: Not a valid file. path=" + path);
            return false;
        }

        PluginInfo info = PluginManagerServer.installLocked(DPluginInternal.getAppContext(), path, pluginName);
        if (info != null) {
            DPlugin.getConfig().getEventCallbacks().onInstallPluginSucceed(info);
            //开始加载dex
            boolean result = new PluginLoader(DPluginInternal.getAppContext(), info).loadLocked();
            if (result) {
                DPlugin.getConfig().getEventCallbacks().onLoadPluginSucceed(info);
            } else {
                DPlugin.getConfig().getEventCallbacks().onLoadPluginFailed(info);
            }
            return result;
        }
        return false;
    }

    /**
     * 删除插件 <p>
     * 注意： <p>
     * 理论上不存在卸载一说，因为已加载的类没法卸载
     * 这里只是删除插件相关的文件信息
     *
     * @param pi 插件信息
     */
    public static void uninstall(PluginInfo pi) {
        PluginManagerServer.delete(pi);
    }

    private static PluginConfig sConfig;

    public static PluginConfig getConfig() {
        return sConfig;
    }

    public static boolean isPluginRunning(String pluginName) {
        return LoadedPluginManager.instance().lookupPlugin(pluginName) != null;
    }

    public static File getInstalledPluginFile(String pluginName, String pluginVer) {
        return new File(PluginInfo.getApkDir(), PluginInfo.makeInstalledFileName(pluginName, pluginVer) + ".jar");
    }

    /**
     * 所有针对Application的调用应从此类开始
     */
    public static class App {

        static boolean sAttached;

        /**
         * 当Application的attachBaseContext调用时需调用此方法 <p>
         * 使用插件框架默认的方案
         *
         * @param app Application对象
         */
        public static void attachBaseContext(Application app) {
            attachBaseContext(app, new PluginConfig());
        }

        /**
         * （推荐）当Application的attachBaseContext调用时需调用此方法 <p>
         *
         * @param app Application对象
         * @see PluginConfig
         */
        public static void attachBaseContext(Application app, PluginConfig config) {
            if (sAttached) {
                PluginLogUtil.d(TAG, "attachBaseContext: Already called");
                return;
            }
            DPluginInternal.init(app);
            sConfig = config;
            sConfig.initDefaults(app);

            HookClassLoaderUtils.hook(app);
            LoadedPluginManager.instance().setHostResources(app.getResources());

            sAttached = true;
        }
    }
}
