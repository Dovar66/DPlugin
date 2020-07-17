package com.dovar.dplugin.plugin;

import android.annotation.TargetApi;
import android.content.res.AssetFileDescriptor;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Movie;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.TypedValue;

import java.io.InputStream;

/**
 * 独立插件的activity/service使用的Resources
 * <p>
 * 构造时使用插件的Assets，所以插件中只能访问到插件的assets，而宿主只能访问宿主的assets
 * <p>
 * 这个类不对外暴露
 */
class SPluginResources extends Resources {
    private Resources hostRes;

    SPluginResources(Resources hostRes, Resources pluginRes) {
        super(pluginRes.getAssets(), hostRes.getDisplayMetrics(), hostRes.getConfiguration());
        this.hostRes = hostRes;
    }

    @NonNull
    @Override
    public CharSequence getText(int id) throws NotFoundException {
        try {
            return super.getText(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getText(id);
        }
    }

    @NonNull
    @Override
    public String getString(int id) throws NotFoundException {
        try {
            return super.getString(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getString(id);
        }
    }


    @Override
    public String getString(int id, Object... formatArgs) throws NotFoundException {
        try {
            return super.getString(id, formatArgs);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getString(id, formatArgs);
        }
    }

    @Override
    public float getDimension(int id) throws NotFoundException {
        try {
            return super.getDimension(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getDimension(id);
        }
    }

    @Override
    public int getDimensionPixelOffset(int id) throws NotFoundException {
        try {
            return super.getDimensionPixelOffset(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getDimensionPixelOffset(id);
        }
    }

    @Override
    public int getDimensionPixelSize(int id) throws NotFoundException {
        try {
            return super.getDimensionPixelSize(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getDimensionPixelSize(id);
        }
    }

    @Override
    public Drawable getDrawable(int id) throws NotFoundException {
        try {
            return super.getDrawable(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getDrawable(id);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Drawable getDrawable(int id, Theme theme) throws NotFoundException {
        try {
            return super.getDrawable(id, theme);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getDrawable(id, theme);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    @Override
    public Drawable getDrawableForDensity(int id, int density) throws NotFoundException {
        try {
            return super.getDrawableForDensity(id, density);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getDrawableForDensity(id, density);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Drawable getDrawableForDensity(int id, int density, Theme theme) {
        try {
            return super.getDrawableForDensity(id, density, theme);
        } catch (Exception e) {
            if (hostRes == null) throw e;
            return hostRes.getDrawableForDensity(id, density, theme);
        }
    }

    @Override
    public int getColor(int id) throws NotFoundException {
        try {
            return super.getColor(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getColor(id);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public int getColor(int id, Theme theme) throws NotFoundException {
        try {
            return super.getColor(id, theme);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getColor(id, theme);
        }
    }

    @Override
    public ColorStateList getColorStateList(int id) throws NotFoundException {
        try {
            return super.getColorStateList(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getColorStateList(id);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public ColorStateList getColorStateList(int id, Theme theme) throws NotFoundException {
        try {
            return super.getColorStateList(id, theme);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getColorStateList(id, theme);
        }
    }

    @Override
    public boolean getBoolean(int id) throws NotFoundException {
        try {
            return super.getBoolean(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getBoolean(id);
        }
    }

    @Override
    public XmlResourceParser getLayout(int id) throws NotFoundException {
        try {
            return super.getLayout(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getLayout(id);
        }
    }

    @Override
    public String getResourceName(int resid) throws NotFoundException {
        try {
            return super.getResourceName(resid);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getResourceName(resid);
        }
    }

    @Override
    public int getInteger(int id) throws NotFoundException {
        try {
            return super.getInteger(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getInteger(id);
        }
    }

    @Override
    public CharSequence getText(int id, CharSequence def) {
        try {
            return super.getText(id, def);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getText(id, def);
        }
    }

    @Override
    public InputStream openRawResource(int id) throws NotFoundException {
        try {
            return super.openRawResource(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.openRawResource(id);
        }

    }

    @Override
    public XmlResourceParser getXml(int id) throws NotFoundException {
        try {
            return super.getXml(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getXml(id);
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    @Override
    public Typeface getFont(int id) throws NotFoundException {
        try {
            return super.getFont(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getFont(id);
        }
    }

    @Override
    public Movie getMovie(int id) throws NotFoundException {
        try {
            return super.getMovie(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getMovie(id);
        }
    }

    @Override
    public XmlResourceParser getAnimation(int id) throws NotFoundException {
        try {
            return super.getAnimation(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.getAnimation(id);
        }
    }

    @Override
    public InputStream openRawResource(int id, TypedValue value) throws NotFoundException {
        try {
            return super.openRawResource(id, value);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.openRawResource(id, value);
        }
    }

    @Override
    public AssetFileDescriptor openRawResourceFd(int id) throws NotFoundException {
        try {
            return super.openRawResourceFd(id);
        } catch (NotFoundException e) {
            if (hostRes == null) throw e;
            return hostRes.openRawResourceFd(id);
        }
    }
}
