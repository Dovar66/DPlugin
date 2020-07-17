package com.dovar.dplugin.core.resources;

import android.annotation.TargetApi;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.ArrayMap;

import com.dovar.dplugin.core.util.Reflector;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

/**
 * Created by weizonghe on 2020/5/11.
 */
public class ResourcesManager {

    public static final String TAG = "LoadedPlugin";

//    private static Configuration mDefaultConfiguration;

    public static synchronized void updateResources(Application hostContext, String newAssetPath) throws Exception {
        //todo 查看热修复后会有几个Resources
//        printResources("hwz");

        //注意context不同，hook的内容也有所不同
        Context c = hostContext.getBaseContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createResourcesForN(c, newAssetPath);
            return;
        }

        ResourcesManager.createResourcesSimple(hostContext, newAssetPath);
    }

    private static void createResourcesSimple(Application hostContext, String newAssetPath) throws Exception {
        Resources hostResources = hostContext.getResources();
        AssetManager assetManager;
        Reflector reflector = Reflector.on(AssetManager.class).method("addAssetPath", String.class);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //5.0及之前AssetManager伪动态，所以需要新建AssetManager
            assetManager = newAssetManager(hostContext);
            reflector.bind(assetManager);
            final int cookie1 = reflector.call(hostContext.getApplicationInfo().sourceDir);
            if (cookie1 == 0) {
                throw new RuntimeException("createResources failed, can't addAssetPath for " + hostContext.getApplicationInfo().sourceDir);
            }
        } else {
            //高版本AssetManager支持动态增加assetPath，所以只要调用addAssetPath就行
            assetManager = hostResources.getAssets();
            reflector.bind(assetManager);
        }
        final int cookie2 = reflector.call(newAssetPath);
        if (cookie2 == 0) {
            throw new RuntimeException("createResources failed, can't addAssetPath for " + newAssetPath);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            try {
                //新创建的AssetManager需要调用ensureStringBlocks()完成初始化，这个初始化原本是在Resources的构造函数中执行的。
                //改成assetManager.getClass().getDeclaredMethod("ensureStringBlocks")是不是更准确点？
                Method mEnsureStringBlocks = AssetManager.class.getDeclaredMethod("ensureStringBlocks", new Class[0]);
                mEnsureStringBlocks.setAccessible(true);
                mEnsureStringBlocks.invoke(assetManager, new Object[0]);

                //更新Resources对象的mAssets
                Collection<WeakReference<Resources>> references = findOutCachedResources(hostContext);
                for (WeakReference<Resources> wr : references) {
                    Resources resources = wr.get();
                    if (resources == null) continue;

                    try {
                        Field mAssets = Resources.class.getDeclaredField("mAssets");
                        mAssets.setAccessible(true);
                        mAssets.set(resources, assetManager);
                    } catch (Throwable ignore) {
                        Field mResourcesImpl = Resources.class.getDeclaredField("mResourcesImpl");
                        mResourcesImpl.setAccessible(true);
                        Object resourceImpl = mResourcesImpl.get(resources);
                        Field implAssets = resourceImpl.getClass().getDeclaredField("mAssets");
                        implAssets.setAccessible(true);
                        implAssets.set(resourceImpl, assetManager);
                    }

                    resources.updateConfiguration(resources.getConfiguration(), resources.getDisplayMetrics());
                }
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        } else {
            //只更新宿主app的Resources的话，会不会跟tinker的资源修复存在冲突？
        }
    }

    /**
     * 方式一
     * <p>
     * Use System Apis to update all existing resources.
     * <br/>
     * 1. Update ApplicationInfo.splitSourceDirs and LoadedApk.mSplitResDirs
     * <br/>
     * 2. Replace all keys of ResourcesManager.mResourceImpls to new ResourcesKey
     * <br/>
     * 3. Use ResourcesManager.appendLibAssetForMainAssetPath(appInfo.publicSourceDir, "${packageName}.vastub") to update all existing resources.
     * <br/>
     * <p>
     * see android.webkit.WebViewDelegate.addWebViewAssetPath(Context)
     */
    @TargetApi(Build.VERSION_CODES.N)
    private static void createResourcesForN(Context context, String newAssetPath) {
        addPackageSplitResDirsByApplicationBaseContext(context, newAssetPath);
        addAssetPathForAndroidN(context, newAssetPath);

//        final android.app.ResourcesManager resourcesManager = android.app.ResourcesManager.getInstance();
//        ArrayMap<ResourcesKey, WeakReference<ResourcesImpl>> originalMap = Reflector.with(resourcesManager).field("mResourceImpls").get();
//
//        synchronized (resourcesManager) {
//            HashMap<ResourcesKey, WeakReference<ResourcesImpl>> resolvedMap = new HashMap<>();
//
//            if (Build.VERSION.SDK_INT >= 28
//                    || (Build.VERSION.SDK_INT == 27 && Build.VERSION.PREVIEW_SDK_INT != 0)) { // P Preview
//                ResourcesManagerCompatForP.resolveResourcesImplMap(originalMap, resolvedMap, context);
//            } else {
//                ResourcesManagerCompatForN.resolveResourcesImplMap(originalMap, resolvedMap, baseResDir, newAssetPath);
//            }
//
//            originalMap.clear();
//            originalMap.putAll(resolvedMap);
//        }
//
//        android.app.ResourcesManager.getInstance().appendLibAssetForMainAssetPath(baseResDir, newAssetPath);
    }

    /**
     * 方式二
     */

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void createResourcesForNByApplication(Application context, String newAssetPath) {
        //这三个方法要在主线程执行
        addPackageSplitResDirsByApplication(context, newAssetPath);
//        addAssetPathToSharedLibrary(context, newAssetPath);
        addAssetPathForAndroidN(context, newAssetPath);
    }

    private static String[] append(String[] paths, String newPath) {
        if (contains(paths, newPath)) {
            return paths;
        }

        final int newPathsCount = 1 + (paths != null ? paths.length : 0);
        final String[] newPaths = new String[newPathsCount];
        if (paths != null) {
            System.arraycopy(paths, 0, newPaths, 0, paths.length);
        }
        newPaths[newPathsCount - 1] = newPath;
        return newPaths;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static boolean contains(String[] array, String value) {
        if (array == null) {
            return false;
        }
        for (int i = 0; i < array.length; i++) {
            if (Objects.equals(array[i], value)) {
                return true;
            }
        }
        return false;
    }

//    private static final class ResourcesManagerCompatForN {
//
//        @TargetApi(Build.VERSION_CODES.KITKAT)
//        private static void resolveResourcesImplMap(Map<ResourcesKey, WeakReference<ResourcesImpl>> originalMap, Map<ResourcesKey, WeakReference<ResourcesImpl>> resolvedMap, String baseResDir, String newAssetPath) throws Exception {
//            for (Map.Entry<ResourcesKey, WeakReference<ResourcesImpl>> entry : originalMap.entrySet()) {
//                ResourcesKey key = entry.getKey();
//                if (Objects.equals(key.mResDir, baseResDir)) {
//                    resolvedMap.put(new ResourcesKey(key.mResDir,
//                            append(key.mSplitResDirs, newAssetPath),
//                            key.mOverlayDirs,
//                            key.mLibDirs,
//                            key.mDisplayId,
//                            key.mOverrideConfiguration,
//                            key.mCompatInfo), entry.getValue());
//                } else {
//                    resolvedMap.put(key, entry.getValue());
//                }
//            }
//        }
//    }
//
//    private static final class ResourcesManagerCompatForP {
//
//        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
//        private static void resolveResourcesImplMap(Map<ResourcesKey, WeakReference<ResourcesImpl>> originalMap, Map<ResourcesKey, WeakReference<ResourcesImpl>> resolvedMap, Context context) throws Exception {
//            HashMap<ResourcesImpl, Context> newResImplMap = new HashMap<>();
//            Map<ResourcesImpl, ResourcesKey> resKeyMap = new HashMap<>();
//            Resources newRes;
//
//            // Recreate the resImpl of the context
//
//            // See LoadedApk.getResources()
//            if (mDefaultConfiguration == null) {
//                mDefaultConfiguration = new Configuration();
//            }
//            newRes = context.createConfigurationContext(mDefaultConfiguration).getResources();
//            newResImplMap.put(newRes.getImpl(), context);
//
//            // Recreate the ResImpl of the activity
//            for (Map.Entry<String, Activity> entry : PluginActivityLifecycleCallback.activities.entrySet()
//            ) {
//                Activity activity = entry.getValue();
//                if (activity != null) {
//                    newRes = activity.createConfigurationContext(activity.getResources().getConfiguration()).getResources();
//                    newResImplMap.put(newRes.getImpl(), activity);
//                }
//            }
//
//            // Mapping all resKey and resImpl
//            for (Map.Entry<ResourcesKey, WeakReference<ResourcesImpl>> entry : originalMap.entrySet()) {
//                ResourcesImpl resImpl = entry.getValue().get();
//                if (resImpl != null) {
//                    resKeyMap.put(resImpl, entry.getKey());
//                }
//                resolvedMap.put(entry.getKey(), entry.getValue());
//            }
//
//            // Replace the resImpl to the new resKey and remove the origin resKey
//            for (Map.Entry<ResourcesImpl, Context> entry : newResImplMap.entrySet()) {
//                ResourcesKey newKey = resKeyMap.get(entry.getKey());
//                ResourcesImpl originResImpl = entry.getValue().getResources().getImpl();
//
//                resolvedMap.put(newKey, new WeakReference<>(originResImpl));
//                resolvedMap.remove(resKeyMap.get(originResImpl));
//            }
//        }
//    }

    private static AssetManager newAssetManager(Context hostContext) {
        try {
            AssetManager assets = newBaiduAssetManager(hostContext);
            if (assets == null) {
                return AssetManager.class.newInstance();
            }
        } catch (InstantiationException e1) {
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            e1.printStackTrace();
        }
        return null;
    }

    private static AssetManager newBaiduAssetManager(Context hostContext) {
        try {
            if ("android.content.res.BaiduAssetManager".equals(hostContext.getAssets().getClass().getName())) {
                return (AssetManager) Class.forName("android.content.res.BaiduAssetManager").newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Collection<WeakReference<Resources>> findOutCachedResources(Application app) throws Exception {
        Collection<WeakReference<Resources>> references;
        if (Build.VERSION.SDK_INT >= 19) {
            Class<?> resourcesManagerClass = Class.forName("android.app.ResourcesManager");
            Method mGetInstance = resourcesManagerClass.getDeclaredMethod("getInstance", new Class[0]);
            mGetInstance.setAccessible(true);
            Object resourcesManager = mGetInstance.invoke(null, new Object[0]);
            try {
                Field fMActiveResources = resourcesManagerClass.getDeclaredField("mActiveResources");
                fMActiveResources.setAccessible(true);

                ArrayMap<?, WeakReference<Resources>> arrayMap = (ArrayMap) fMActiveResources.get(resourcesManager);

                references = arrayMap.values();
            } catch (NoSuchFieldException ignore) {
                Field mResourceReferences = resourcesManagerClass.getDeclaredField("mResourceReferences");
                mResourceReferences.setAccessible(true);

                references = (Collection) mResourceReferences.get(resourcesManager);
            }
        } else {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Field fMActiveResources = activityThread.getDeclaredField("mActiveResources");
            fMActiveResources.setAccessible(true);
            Object thread = getActivityThread(app, activityThread);

            HashMap<?, WeakReference<Resources>> map = (HashMap) fMActiveResources.get(thread);

            references = map.values();
        }
        return references;
    }

    private static Object getActivityThread(Context context, Class<?> activityThread) {
        try {
            // ActivityThread.currentActivityThread()
            Method m = activityThread.getMethod("currentActivityThread", new Class[0]);
            m.setAccessible(true);
            Object thread = m.invoke(null, new Object[0]);
            if (thread != null) return thread;
            // context.@mLoadedApk.@mActivityThread
            Field mLoadedApk = context.getClass().getField("mLoadedApk");
            mLoadedApk.setAccessible(true);
            Object apk = mLoadedApk.get(context);
            Field mActivityThreadField = apk.getClass().getDeclaredField("mActivityThread");
            mActivityThreadField.setAccessible(true);
            return mActivityThreadField.get(apk);
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static void printResources(String mark, Application app) {
        try {
            Collection<WeakReference<Resources>> references = findOutCachedResources(app);
            for (WeakReference<Resources> wr : references) {
                Resources resources = wr.get();
                if (resources == null) continue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addPackageSplitResDirsByApplication(Application context, String newAssetPath) {
        try {
            Field fieldLoadedApk = context.getClass().getField("mLoadedApk");
            fieldLoadedApk.setAccessible(true);
            Object packageInfo = fieldLoadedApk.get(context);
            Field fieldSplitResDirs = packageInfo.getClass().getDeclaredField("mSplitResDirs");
            fieldSplitResDirs.setAccessible(true);
            String[] splitResDirs = (String[]) fieldSplitResDirs.get(packageInfo);
            String[] newSplitResDirs = new String[(splitResDirs == null ? 0 : splitResDirs.length) + 1];
            if (splitResDirs != null && newSplitResDirs.length > 1) {
                System.arraycopy(splitResDirs, 0, newSplitResDirs, 0, splitResDirs.length);
            }
            newSplitResDirs[newSplitResDirs.length - 1] = newAssetPath;
            fieldSplitResDirs.set(packageInfo, newSplitResDirs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addPackageSplitResDirsByApplicationBaseContext(Context context, String newAssetPath) {
        try {
            Object loadedApk = Reflector.with(context).field("mPackageInfo").get();
            Reflector rLoadedApk = Reflector.with(loadedApk).field("mSplitResDirs");
            String[] splitResDirs = rLoadedApk.get();
            rLoadedApk.set(append(splitResDirs, newAssetPath));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addAssetPathToSharedLibrary(Context context, String newAssetPath) {
        final ApplicationInfo appInfo = context.getApplicationInfo();
        final String[] libs = appInfo.sharedLibraryFiles;
        final int newLibAssetsCount = 1 + (libs != null ? libs.length : 0);
        final String[] newLibAssets = new String[newLibAssetsCount];
        if (libs != null) {
            System.arraycopy(libs, 0, newLibAssets, 0, libs.length);
        }
        newLibAssets[newLibAssetsCount - 1] = newAssetPath;

        // Update the ApplicationInfo object with the new list.
        // We know this will persist and future Resources created via ResourcesManager
        // will include the shared library because this ApplicationInfo comes from the
        // underlying LoadedApk in ContextImpl, which does not change during the life of the
        // application.
        if (libs != null) {
            for (String path : libs) {
                if (path.equals(newAssetPath)) {
                    return;
                }
            }
        }
        appInfo.sharedLibraryFiles = newLibAssets;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private static void addAssetPathForAndroidN(Context context, String newAssetPath) {
        final ApplicationInfo appInfo = context.getApplicationInfo();
        String baseResDir = appInfo.publicSourceDir;
        appInfo.splitSourceDirs = append(appInfo.splitSourceDirs, newAssetPath);
        try {
            Class ResourcesManagerClass = Class.forName("android.app.ResourcesManager");
            Method getInstanceMethod = ResourcesManagerClass.getDeclaredMethod("getInstance");
            Method appendLibAssetForMainAssetPathMethod = ResourcesManagerClass.getDeclaredMethod("appendLibAssetForMainAssetPath", String.class, String.class);
            Object resourcesManagerObject = getInstanceMethod.invoke(ResourcesManagerClass);


            Field mResourceImplsField = ResourcesManagerClass.getDeclaredField("mResourceImpls");
            mResourceImplsField.setAccessible(true);
            ArrayMap map = (ArrayMap) mResourceImplsField.get(resourcesManagerObject);

            final int refCount = map.size();

            for (int i = 0; i < refCount; i++) {
                Object resourcesKeyObject = map.keyAt(i);
                if (resourcesKeyObject != null) {
                    addToSplitResDirs(resourcesKeyObject, newAssetPath);
                }
            }
            appendLibAssetForMainAssetPathMethod.invoke(resourcesManagerObject, baseResDir, newAssetPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void addToSplitResDirs(Object resourcesKeyObject, String newAssetPath) {
        try {
            Field mSplitResDirsField = resourcesKeyObject.getClass().getDeclaredField("mSplitResDirs");
            mSplitResDirsField.setAccessible(true);

            String[] mSplitResDirs = (String[]) mSplitResDirsField.get(resourcesKeyObject);
            String[] newmSplitResDirs = null;
            if (mSplitResDirs != null) {
                newmSplitResDirs = new String[mSplitResDirs.length + 1];
                System.arraycopy(mSplitResDirs, 0, newmSplitResDirs, 0, mSplitResDirs.length);
                newmSplitResDirs[newmSplitResDirs.length - 1] = newAssetPath;
            } else {
                newmSplitResDirs = new String[]{newAssetPath};
            }

            mSplitResDirsField.set(resourcesKeyObject, newmSplitResDirs);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void checkRes(final String pluginPath) {
        Class<?> handleClazz = null;
        Method createMethod = null;
        try {
            handleClazz = Class.forName("com.android.internal.content.NativeLibraryHelper$Handle");
            createMethod = handleClazz.getDeclaredMethod("create", File.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (createMethod != null) {
            try {
                File pluginFile = new File(pluginPath);
                createMethod.invoke(handleClazz, pluginFile);
            } catch (Exception e) {
                e.printStackTrace();
                //插件资源加载失败
            }
        }
    }
}
