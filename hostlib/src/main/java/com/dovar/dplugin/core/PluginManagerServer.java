package com.dovar.dplugin.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;

import com.dovar.dplugin.core.entity.PluginInfo;
import com.dovar.dplugin.core.helper.PluginNativeLibsHelper;
import com.dovar.dplugin.core.util.FileUtils;
import com.dovar.dplugin.core.util.PluginLogUtil;

import java.io.File;
import java.io.IOException;


/**
 * 插件管理器。用来控制插件的安装、卸载、获取等。运行在常驻进程中 <p>
 * 补充：涉及到插件交互、运行机制有关的管理器，在IPluginHost中 <p>
 * <p>
 * 注意：插件框架内部使用，外界请不要调用。
 */
public class PluginManagerServer {

    private static final String TAG = "PluginManagerServer";


    public static PluginInfo installLocked(@NonNull Context context, String path, String pluginName) {
        // 1. 读取APK内容
        PackageInfo pi = context.getPackageManager().getPackageArchiveInfo(path, PackageManager.GET_META_DATA);
        if (pi == null) {
            PluginLogUtil.e(TAG, "installLocked: Not a valid apk. path=" + path);
            DPlugin.getConfig().getEventCallbacks().onInstallPluginFailed(path, DPluginEventCallbacks.InstallResult.READ_PKG_INFO_FAIL);
            return null;
        }

        // 2. 校验插件签名

        // 3. 解析出名字和三元组
        PluginInfo instPli = PluginInfo.parseFromPackageInfo(pluginName, pi, path);
        PluginLogUtil.i(TAG, "installLocked: Info=" + instPli);

        //如果插件正在运行就return
        if (DPlugin.isPluginRunning(instPli.getName())) return null;

        // 4. 将合法的APK改名后，移动（或复制）到新位置
        if (!copyOrMoveApk(path, instPli)) {
            DPlugin.getConfig().getEventCallbacks().onInstallPluginFailed(path, DPluginEventCallbacks.InstallResult.COPY_APK_FAIL);
            return null;
        }

        // 5. 从插件中释放 So 文件
        PluginNativeLibsHelper.install(instPli.getPath(), instPli.getNativeLibsDir());

        return instPli;
    }

    private static boolean copyOrMoveApk(String path, PluginInfo instPli) {
        File srcFile = new File(path);
        File newFile = instPli.getApkFile();

        if (!newFile.exists()) {
            try {
                // 将源APK文件移动/复制到安装路径下
                FileUtils.moveFile(srcFile, newFile);
            } catch (IOException e) {
                PluginLogUtil.e(TAG, "copyOrMoveApk: Copy/Move Failed! src=" + srcFile + "; dest=" + newFile);
                return false;
            }
        }

        instPli.setPath(newFile.getAbsolutePath());
        return true;
    }

    public static void delete(@NonNull PluginInfo pi) {
        try {
            FileUtils.forceDelete(new File(pi.getPath()));
            FileUtils.forceDelete(pi.getDexFile());
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                FileUtils.forceDelete(pi.getExtraOdexDir());
            }
            FileUtils.forceDelete(pi.getNativeLibsDir());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e2) {
            e2.printStackTrace();
        }
    }
}
