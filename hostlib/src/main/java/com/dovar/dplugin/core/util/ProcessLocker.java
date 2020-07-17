package com.dovar.dplugin.core.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * 进程锁
 */
public final class ProcessLocker {

    private static final String TAG = "ProcessLocker";

    private final Context mContext;

    private FileOutputStream mFileOutputStream;

    private FileChannel mFileChannel;

    private FileLock mFileLock;

    private File mFile;

    /**
     * @param context
     * @param filename
     */
    public ProcessLocker(Context context, String filename) {
        mContext = context;
        try {
            mFile = new File(filename);
            mFileOutputStream = mContext.openFileOutput(filename, 0);
            if (mFileOutputStream != null) {
                mFileChannel = mFileOutputStream.getChannel();
            }
            if (mFileChannel == null) {
                PluginLogUtil.e("channel is null");
            }
        } catch (Throwable e) {
            PluginLogUtil.e(e.getMessage());
        }
    }

    /**
     * 允许传递绝对路径
     *
     * @param context
     * @param dir
     * @param filename
     */
    public ProcessLocker(Context context, String dir, String filename) {
        mContext = context;
        try {
            mFile = new File(dir, filename);
            if (!mFile.exists()) {
                FileUtils.forceMkdirParent(mFile);
                mFile.createNewFile();
            }
            mFileOutputStream = new FileOutputStream(mFile, false);
            mFileChannel = mFileOutputStream.getChannel();
        } catch (Throwable e) {
            PluginLogUtil.e(e.getMessage());
        }
    }

    /**
     * 查看文件是否已经被上锁
     *
     * @return
     */
    public final synchronized boolean isLocked() {
        boolean ret = tryLock();

        // 加锁成功说明文件还未被上锁
        // 在退出之前一定要进行unlock
        if (ret) {
            unlock();
        }

        return !ret;
    }

    /**
     * 加锁
     *
     * @return
     */
    public final synchronized boolean tryLock() {
        if (mFileChannel == null) {
            return false;
        }
        try {
            mFileLock = mFileChannel.tryLock();
            if (mFileLock != null) {
                return true;
            }
        } catch (Throwable e) {
            PluginLogUtil.e(e.getMessage());
        }
        return false;
    }

    /**
     * 加锁
     *
     * @param ms       毫秒
     * @param interval 间隔
     * @return
     */
    public final synchronized boolean tryLockTimeWait(int ms, int interval) {
        if (mFileChannel == null) {
            return false;
        }
        // 自动修正到最小值，避免死锁
        if (ms <= 0) {
            ms = 1;
        }
        if (interval <= 0) {
            interval = 1;
        }
        try {
            for (int i = 0; i < ms; i += interval) {
                try {
                    mFileLock = mFileChannel.tryLock();
                } catch (IOException e) {
                    // 获取锁失败会抛异常，此处忽略
                    // java.io.IOException: fcntl failed: EAGAIN (Try again)
                }
                if (mFileLock != null) {
                    return true;
                }
                Thread.sleep(interval, 0);
            }
        } catch (Throwable e) {
            PluginLogUtil.e(e.getMessage());
        }
        return false;
    }

    /**
     * 加锁
     *
     * @return
     */
    public final synchronized boolean lock() {
        if (mFileChannel == null) {
            return false;
        }
        try {
            mFileLock = mFileChannel.lock();
            if (mFileLock != null) {
                return true;
            }
        } catch (Throwable e) {
            PluginLogUtil.e(e.getMessage());
        }
        return false;
    }

    /**
     * 释放并且删除该锁文件
     */
    public final synchronized void unlock() {
        if (mFileLock != null) {
            try {
                mFileLock.release();
            } catch (Throwable e) {
                PluginLogUtil.e(TAG, e.getMessage());
            }
        }
        if (mFileChannel != null) {
            try {
                mFileChannel.close();
            } catch (Throwable e) {
                PluginLogUtil.e(TAG, e.getMessage());
            }
        }
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.close();
            } catch (Throwable e) {
                PluginLogUtil.e(TAG, e.getMessage());
            }
        }

        // 删除锁文件
        if (mFile != null && mFile.exists()) {
            mFile.delete();
        }
    }
}
