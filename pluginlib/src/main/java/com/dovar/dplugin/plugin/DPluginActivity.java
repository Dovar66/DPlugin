package com.dovar.dplugin.plugin;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;

import com.dovar.dplugin.core.LoadedPluginManager;
import com.dovar.dplugin.core.PluginParts;

/**
 * 独立插件中所有Activity的基类
 * <p>
 * 插件中的类一定是插件PluginClassLoader加载出来的，可以通过classLoader去找到该插件的PluginParts
 */
public class DPluginActivity extends FragmentActivity {

    private Resources mixResources;

    /**
     * 如果Context的mResources在attachBaseContext之前就已经赋值，那我们就只能通过反射去修改mResources了
     * <p>
     * 当然，理论上不可能是这种情况，因为创建当前Context的mResources需要使用mBase，而此处才是对mBase赋值。
     * <p>
     * DPluginService同理
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        PluginParts parts = LoadedPluginManager.instance().lookupPlugin(getClass().getClassLoader());
        if (parts != null && parts.resources != null) {
            if (parts.mode == PluginParts.MODE_INDEPENDENT) {
                //系统会在插件中获取Application的icon，所以还是要访问宿主资源
                Resources host = LoadedPluginManager.instance().getHostResources();
                mixResources = new SPluginResources(host, parts.resources);
            } else {
                //耦合插件不需要继承 DPluginActivity
                //即使继承了，这里也啥都不干
            }
        }
        super.attachBaseContext(newBase);
    }

    /**
     * 系统调用getResourcesInternal获取Context的mResources时，在特定厂商ROM会直接强转成类似VivoResources/MiuiResources这样的
     * <p>
     * 所以如果mBase的getResources()返回的是自定义的Resources对象那此时就会发生类型转换错误
     * <p>
     * 所以要对插件的Context的mBase进行修改，使其getResources返回的是系统API创建的Resources对象
     * <p>
     * 注意：如果发现ROM会调用getResources()去进行强制转换，那这种方案就要重新考量了。
     */
    @Override
    public Resources getResources() {
        if (mixResources != null) {
            return mixResources;
        }
        return super.getResources();
    }

    @Override
    public AssetManager getAssets() {
        if (mixResources != null) {
            return mixResources.getAssets();
        }
        return super.getAssets();
    }

  /*
    @Override
    public ApplicationInfo getApplicationInfo() {
        PluginLogUtil.d("hwz", "ai" + super.getApplicationInfo().uid);
        if (mixResources != null) {
            return DPluginInternal.getAppContext().getApplicationInfo();
        }
        return super.getApplicationInfo();
    }

    *//**
     * 运行在插件时，应该返回插件的还是宿主的？
     *//*
    @Override
    public String getPackageName() {
        PluginLogUtil.d("hwz", "pn" + super.getPackageName());
        if (mixResources != null) {
            return DPluginInternal.getAppContext().getPackageName();
        }
        return super.getPackageName();
    }

    *//**
     * packageManager需不需要处理？
     *
     * @return
     *//*
    @Override
    public PackageManager getPackageManager() {
        PluginLogUtil.d("hwz", "pm:" + super.getPackageManager().toString());
        if (mixResources != null) {
            return DPluginInternal.getAppContext().getPackageManager();
        }
        return super.getPackageManager();
    }*/

/*    @Override
    public void setTheme(@Nullable Resources.Theme theme) {
        Log.d("hwz", "setTheme: ");
        super.setTheme(theme);
    }

    *//**
     * 调用顺序：attachBaseContext->setTheme->onCreate->getTheme
     *//*
    @Override
    public void setTheme(int resid) {
        Log.d("hwz", "setTheme: " + resid);

        super.setTheme(resid);
    }

    @Override
    public Resources.Theme getTheme() {
        Log.d("hwz", "getTheme: ");
        return super.getTheme();
    }*/
}
