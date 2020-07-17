package com.dovar.dplugin.core.entity;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.dovar.dplugin.core.DPluginInternal;
import com.dovar.dplugin.core.helper.VMRuntimeCompat;
import com.dovar.dplugin.core.util.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * 用来描述插件的描述信息。以Json来封装
 */
public class PluginInfo implements Serializable, Parcelable, Cloneable {

    private static final long serialVersionUID = -6531475023210445876L;

    private static final String PI_PKGNAME = "pkgname"; // 插件包名作为插件的唯一标识
    private static final String PI_NAME = "name"; // 插件包名作为插件的唯一标识
    private static final String PI_VER = "ver"; // 插件版本
    private static final String PI_PATH = "path"; //插件apk的路径
    private static final String PI_MODE = "mode";//0.独立插件 1.耦合插件
    private static final String PI_WHITELIST = "whitelist";//白名单

    private transient final Map<String, Object> mJson = new ConcurrentHashMap(1 << 4);

    private PluginInfo(JSONObject jo) {
        initPluginInfo(jo);
    }

    private PluginInfo(String pluginName, String pkgNames, String version, String path, int mode, String whiteList) {
        put(PI_NAME, pluginName);
        put(PI_PKGNAME, pkgNames);
        put(PI_WHITELIST, whiteList);
        put(PI_VER, version);
        put(PI_MODE, mode);

        setPath(path);
    }

    private void initPluginInfo(JSONObject jo) {
        final Iterator<String> keys = jo.keys();
        while (keys.hasNext()) {
            final String k = keys.next();
            put(k, jo.opt(k));
        }
    }

    /**
     * 通过插件APK的MetaData来初始化PluginInfo <p>
     * 注意：框架内部接口，外界请不要直接使用
     */
    public static PluginInfo parseFromPackageInfo(String pluginName, PackageInfo pi, String path) {
        ApplicationInfo ai = pi.applicationInfo;
        String pn = "";
        String whiteList = "";
        String ver = "";
        int mode = 0;
        Bundle metaData = ai.metaData;

        // 优先读取MetaData中的内容（如有），并覆盖上面的默认值
        if (metaData != null) {
            pn = metaData.getString("com.dovar.plugin.pkgs");
            whiteList = metaData.getString("com.dovar.plugin.whitelist");

            // 获取插件的版本号。优先从metaData中读取，如无则使用插件的VersionCode
            ver = metaData.getString("com.dovar.plugin.version");
            mode = metaData.getInt("com.dovar.plugin.mode");
        }

        return new PluginInfo(pluginName, pn, ver, path, mode, whiteList);
    }

    /**
     * 获取插件名
     */
    public String getName() {
        return get(PI_NAME, "");
    }

    /**
     * 获取插件包名
     */
    public String getPackageName() {
        return get(PI_PKGNAME, "");
    }

    public String getWhiteList() {
        return get(PI_WHITELIST, "");
    }

    /**
     * 获取插件的版本
     */
    public String getVersion() {
        return get(PI_VER, "");
    }

    /**
     * 获取最新的插件，目前所在的位置
     */
    public String getPath() {
        return get(PI_PATH, "");
    }

    /**
     * 设置最新的插件，目前所在的位置 <p>
     * 注意：若为“纯APK”方案所用，则修改后需调用PluginInfoList.save来保存，否则会无效
     */
    public void setPath(String path) {
        put(PI_PATH, path);
    }

    /**
     * 插件的Dex是否已被优化（释放）了？
     *
     * @return
     */
    public boolean isDexExtracted() {
        File f = getDexFile();
        // 文件存在，且大小不为 0 时，才返回 true。
        return f.exists() && FileUtils.sizeOf(f) > 0;
    }

    /**
     * 获取APK存放的文件信息 <p>
     *
     * @return Apk所在的File对象
     */
    public File getApkFile() {
        return new File(getApkDir(), makeInstalledFileName() + ".jar");
    }

    /**
     * 获取APK存放目录
     */
    public static String getApkDir() {
        // 必须使用宿主的Context对象，防止出现“目录定位到插件内”的问题
        Context context = DPluginInternal.getAppContext();
        File dir = context.getDir(Constant.LOCAL_PLUGIN_APK_SUB_DIR, 0);
        return dir.getAbsolutePath();
    }

    /**
     * 获取或创建（如果需要）某个插件的Dex目录，用于放置dex文件
     * 注意：仅供框架内部使用;仅适用于Android 4.4.x及以下
     *
     * @param dirSuffix 目录后缀
     * @return 插件的Dex所在目录的File对象
     */
    @NonNull
    private File getDexDir(File dexDir, String dirSuffix) {
        File dir = new File(dexDir, makeInstalledFileName() + dirSuffix);

        if (!dir.exists()) {
            dir.mkdir();
        }
        return dir;
    }

    /**
     * 获取Extra Dex（优化前）生成时所在的目录 <p>
     * 注意：仅供框架内部使用;仅适用于Android 4.4.x及以下
     *
     * @return 优化前Extra Dex所在目录的File对象
     */
    public File getExtraDexDir() {
        return getDexDir(getDexParentDir(), Constant.LOCAL_PLUGIN_INDEPENDENT_EXTRA_DEX_SUB_DIR);
    }

    /**
     * 获取Extra Dex（优化后）生成时所在的目录 <p>
     * 注意：仅供框架内部使用;仅适用于Android 4.4.x及以下
     *
     * @return 优化后Extra Dex所在目录的File对象
     */
    public File getExtraOdexDir() {
        return getDexDir(getDexParentDir(), Constant.LOCAL_PLUGIN_INDEPENDENT_EXTRA_ODEX_SUB_DIR);
    }

    /**
     * 获取Dex（优化后）生成时所在的目录 <p>
     * <p>
     * Android O之前：
     * 位于app_p_odex中 <p>
     * <p>
     * Android O：
     * APK存放目录/oat/{cpuType}
     * <p>
     * 注意：仅供框架内部使用
     *
     * @return 优化后Dex所在目录的File对象
     */
    public File getDexParentDir() {
        // 必须使用宿主的Context对象，防止出现“目录定位到插件内”的问题
        Context context = DPluginInternal.getAppContext();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            return new File(getApkDir() + File.separator + "oat" + File.separator + VMRuntimeCompat.getArtOatCpuType());
        } else {
            return context.getDir(Constant.LOCAL_PLUGIN_APK_ODEX_SUB_DIR, 0);
        }
    }

    /**
     * 获取Dex（优化后）所在的文件信息 <p>
     * <p>
     * Android O 之前：
     * 位于app_p_odex中<p>
     * <p>
     * Android O：
     * APK存放目录/oat/{cpuType}/XXX.odex
     * <p>
     * 注意：仅供框架内部使用
     *
     * @return 优化后Dex所在文件的File对象
     */
    public File getDexFile() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            File dir = getDexParentDir();
            return new File(dir, makeInstalledFileName() + ".odex");
        } else {
            File dir = getDexParentDir();
            return new File(dir, makeInstalledFileName() + ".dex");
        }
    }

    /**
     * 根据类型来获取SO释放的路径 <p>
     * 位于app_p_so中 <p>
     * 注意：仅供框架内部使用
     *
     * @return SO释放路径所在的File对象
     */
    public File getNativeLibsDir() {
        // 必须使用宿主的Context对象，防止出现“目录定位到插件内”的问题
        Context context = DPluginInternal.getAppContext();
        File dir = context.getDir(Constant.LOCAL_PLUGIN_APK_LIB_DIR, 0);
        return new File(dir, makeInstalledFileName());
    }

    public int getMode() {
        return get(PI_MODE, 0);
    }

    // @hide
    public JSONObject getJSON() {
        return new JSONObject(mJson);
    }

    /**
     * 生成用于放入 app_p_so 等目录下的插件的文件名，其中：<p>
     * 1、得到混淆后的文件名（规则见代码内容） <p>
     * 2、只获取文件名，其目录和扩展名仍需在外面定义
     *
     * @return 文件名（不含扩展名）
     */
    public String makeInstalledFileName() {
        return makeInstalledFileName(getName(), getVersion());
    }

    public static String makeInstalledFileName(String name, String ver) {
        // 混淆插件名字，做法：
        // 1. 生成最初的名字：[插件名（小写）][插件版本]
        //    必须用小写和数字、无特殊字符，否则hashCode后会有一定的重复率
        // 2. 将其生成出hashCode
        // 3. 将整体数字 - 88
        String n = name.toLowerCase() + ver;
        int h = n.hashCode() - 88;
        return Integer.toString(h);
    }

    // -------------------------
    // Parcelable and Cloneable
    // -------------------------

    public static final Creator<PluginInfo> CREATOR = new Creator<PluginInfo>() {

        @Override
        public PluginInfo createFromParcel(Parcel source) {
            return new PluginInfo(source);
        }

        @Override
        public PluginInfo[] newArray(int size) {
            return new PluginInfo[size];
        }
    };

    private PluginInfo(Parcel source) {
        JSONObject jo = null;
        String txt = null;
        try {
            txt = source.readString();
            jo = new JSONObject(txt);
        } catch (JSONException e) {
            jo = new JSONObject();
        }
        initPluginInfo(jo);
    }

    @Override
    public Object clone() {
        try {
            final String jsonText = getJSON().toString();
            return new PluginInfo(new JSONObject(jsonText));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(getJSON().toString());
    }

    @Override
    public int hashCode() {
        return mJson.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (this == obj) {
            return true;
        }

        if (this.getClass() != obj.getClass()) {
            return false;
        }

        PluginInfo pluginInfo = (PluginInfo) obj;

        try {
            return pluginInfo.mJson.toString().equals(mJson.toString());
        } catch (Exception e) {
            return false;
        }
    }

    private <T> T get(String name, @NonNull T def) {
        final Object obj = mJson.get(name);
        return (def.getClass().isInstance(obj)) ? (T) obj : def;
    }

    public <T> void put(String key, T value) {
        if (key == null || value == null) return;
        mJson.put(key, value); //value & key must not null
    }
}
