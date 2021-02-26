package com.dovar.app;

import java.lang.reflect.Method;

public class DynamicAgent {
    private static volatile Object instance;

    public static synchronized void test() {
        try {
            Class<?> c = Class.forName("com.dovar.plugin.TestDynamicImpl");
            if (instance == null) {
                Method method = c.getMethod("getInstance", new Class[]{});
                instance = method.invoke(null);
            }

            Method test = c.getMethod("testLoadClass");
            test.invoke(instance);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

}
