package com.dovar.plugin;

import android.util.Log;

/**
 * 被宿主反射调用，不允许混淆
 */
public class TestDynamicImpl {
    private static class Singleton {
        private static final TestDynamicImpl INSTANCE = new TestDynamicImpl();
    }

    public static TestDynamicImpl getInstance() {
        return TestDynamicImpl.Singleton.INSTANCE;
    }

    private TestDynamicImpl() {
    }

    public void testLoadClass() {
        Log.d("TestDynamicImpl", "插件类加载成功！");
    }
}
