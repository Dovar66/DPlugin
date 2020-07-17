package com.dovar.dplugin.core.loader;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;

import com.dovar.dplugin.core.DPluginInternal;
import com.dovar.dplugin.core.LoadedPluginManager;
import com.dovar.dplugin.core.PluginParts;
import com.dovar.dplugin.core.classloader.ManagerClassLoader;
import com.dovar.dplugin.core.classloader.PluginClassLoader;
import com.dovar.dplugin.core.classloader.SPluginClassLoader;
import com.dovar.dplugin.core.entity.Constant;
import com.dovar.dplugin.core.entity.PluginInfo;
import com.dovar.dplugin.core.resources.ResourcesManager;
import com.dovar.dplugin.core.util.FileUtils;
import com.dovar.dplugin.core.util.ProcessLocker;

import java.io.File;
import java.io.IOException;

public class PluginLoader {

    private PluginInfo mInfo;

    private Context mContext;


    public PluginLoader(Context context, PluginInfo info) {
        mContext = context;
        mInfo = info;
    }

    public boolean loadLocked() {
        Context context = mContext;

        String lockFileName = String.format(Constant.LOAD_PLUGIN_LOCK, mInfo.getApkFile().getName());
        ProcessLocker lock = new ProcessLocker(context, lockFileName);
        lock.tryLockTimeWait(5000, 10);
        boolean rc = false;
        try {
            //首次加载
            rc = doLoad();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        if (rc) {
            // 至此，该插件已开始运行
            return true;
        }

        //重试
        lock = new ProcessLocker(context, lockFileName);
        lock.tryLockTimeWait(5000, 10);
        try {
            // 删除优化dex文件
            File odex = mInfo.getDexFile();
            if (odex.exists()) {
                odex.delete();
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                // support for multidex below LOLLIPOP:delete Extra odex,if need
                try {
                    FileUtils.forceDelete(mInfo.getExtraOdexDir());
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e2) {
                    e2.printStackTrace();
                }
            }
            // 尝试再次加载该插件
            rc = tryLoadAgain();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return rc;
    }

    /**
     * 抽出方法，将mLoader设置为null与doload中mLoader的使用添加同步锁，解决在多线程下导致mLoader为空指针的问题。
     */
    private synchronized boolean tryLoadAgain() {
        return doLoad();
    }

    private boolean doLoad() {
        if (!loadDex()) {
            return false;
        }
        return isDexLoaded();
    }

    private PackageInfo mPackageInfo;
    private Resources mPkgResources;
    private ClassLoader mClassLoader;

    private boolean isDexLoaded() {
        return mPackageInfo != null && mPkgResources != null && mClassLoader != null;
    }

    private boolean loadDex() {
        try {
            String mPath = mInfo.getPath();
            String mPluginPkgName = mInfo.getPackageName();
            PackageManager pm = mContext.getPackageManager();
            if (mPackageInfo == null) {
                mPackageInfo = pm.getPackageArchiveInfo(mPath, PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES | PackageManager.GET_PROVIDERS | PackageManager.GET_RECEIVERS | PackageManager.GET_META_DATA);
                if (mPackageInfo == null || mPackageInfo.applicationInfo == null) {
                    mPackageInfo = null;
                    return false;
                }
                mPackageInfo.applicationInfo.sourceDir = mPath;
                mPackageInfo.applicationInfo.publicSourceDir = mPath;

                if (TextUtils.isEmpty(mPackageInfo.applicationInfo.processName)) {
                    mPackageInfo.applicationInfo.processName = mPackageInfo.applicationInfo.packageName;
                }

                // 添加针对SO库的加载
                File ld = mInfo.getNativeLibsDir();
                mPackageInfo.applicationInfo.nativeLibraryDir = ld.getAbsolutePath();
            }

            // 创建或获取ComponentList表

            //创建Resources
            if (mPkgResources == null) {
                try {
                    mPkgResources = pm.getResourcesForApplication(mPackageInfo.applicationInfo);
                    if (mInfo.getMode() == PluginParts.MODE_COMBINE) {
                        //耦合模式需要处理资源
                        ResourcesManager.updateResources(DPluginInternal.getAppContext(), mPath);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            }

            if (mClassLoader == null) {
                String out = mInfo.getDexParentDir().getPath();
                String soDir = mPackageInfo.applicationInfo.nativeLibraryDir;

                if (mInfo.getMode() == PluginParts.MODE_COMBINE) {
                    //注意不要传了修改后的hostClassLoader
                    mClassLoader = new PluginClassLoader(mPath, out, soDir, getClass().getClassLoader(), DPluginInternal.getManagerClassLoader().getParent(), mPluginPkgName);
                } else {
                    mClassLoader = new SPluginClassLoader(mPath, out, soDir, getClass().getClassLoader(), DPluginInternal.getManagerClassLoader().getParent(), mPluginPkgName, mInfo.getWhiteList());
                }
            }

            boolean result = LoadedPluginManager.instance().installPlugin(mInfo.getName(), mPluginPkgName, mClassLoader, mPkgResources, mInfo);
            if (result) {
                ManagerClassLoader.cachePluginClassLoader(mPluginPkgName, mClassLoader);
            }
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

}
