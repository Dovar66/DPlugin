package com.dovar.dplugin.core.classloader;

import android.app.Application;

import com.dovar.dplugin.core.util.PluginLogUtil;

import java.lang.reflect.Field;

/**
 * 对宿主的HostClassLoader做修改,修改parent域指向ManagerClassLoader
 */
public class HookClassLoaderUtils {

    private static final String TAG = "PatchClassLoaderUtils";

    public static boolean hook(Application application) {
        try {
            hackParentClassLoader(application.getClassLoader(), new ManagerClassLoader(application.getClassLoader().getParent()));

            PluginLogUtil.d(TAG, "patch: patch mClassLoader ok");
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 修改ClassLoader的parent
     *
     * @param classLoader          需要修改的ClassLoader
     * @param newParentClassLoader classLoader的新的parent
     * @throws Exception 失败时抛出
     */
    private static void hackParentClassLoader(ClassLoader classLoader, ClassLoader newParentClassLoader) throws Exception {
        Field field = getParentField(classLoader);
        if (field == null) {
            throw new RuntimeException("在ClassLoader.class中没找到类型为ClassLoader的parent域");
        }
        field.setAccessible(true);
        field.set(classLoader, newParentClassLoader);
    }

    /**
     * 安全地获取到ClassLoader类的parent域
     *
     * @return ClassLoader类的parent域.或不能通过反射访问该域时返回null.
     */
    private static Field getParentField(ClassLoader classLoader) {
        ClassLoader parent = classLoader.getParent();
        Field field = null;
        for (Field f : ClassLoader.class.getDeclaredFields()) {
            try {
                boolean accessible = f.isAccessible();
                f.setAccessible(true);
                Object o = f.get(classLoader);
                f.setAccessible(accessible);
                if (o == parent) {
                    field = f;
                    break;
                }
            } catch (IllegalAccessException ignore) {
            }
        }
        return field;
    }
}
