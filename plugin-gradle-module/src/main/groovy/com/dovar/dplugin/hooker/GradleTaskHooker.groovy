package com.dovar.dplugin.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.BaseVariantData
import com.dovar.dplugin.DExtention
import com.dovar.dplugin.DExtention.VAContext
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Base class of gradle task hooker， provides some common field used by hookers
 * @param <T> Type of hooked task
 */
public abstract class GradleTaskHooker<T extends Task> {

    private Project project

    /**
     * A Build variant when build a apk and all its public data.
     */
    private ApkVariant apkVariant

    private DExtention sDExtention

    private TaskHookerManager taskHookerManager

    public GradleTaskHooker(Project project, ApkVariant apkVariant) {
        this.project = project
        this.apkVariant = apkVariant
        this.sDExtention = project.dpluginConfig

        vaContext.checkList.addCheckPoint(taskName)
    }

    public Project getProject() {
        return this.project
    }

    public ApkVariant getApkVariant() {
        return this.apkVariant
    }

    public BaseVariantData getVariantData() {
        return ((ApplicationVariantImpl) this.apkVariant).variantData
    }

    public VariantScope getScope() {
        return variantData.scope
    }

    public DExtention getPluginConfig() {
        return this.sDExtention
    }

    public VAContext getVaContext() {
        return this.sDExtention.getVaContext(apkVariant.name)
    }

    public void mark() {
        vaContext.checkList.mark(taskName)
    }

    public void setTaskHookerManager(TaskHookerManager taskHookerManager) {
        this.taskHookerManager = taskHookerManager
    }

    public TaskHookerManager getTaskHookerManager() {
        return this.taskHookerManager
    }

    public T getTask() {

    }

    /**
     * Return the transform name of the hooked task(transform task)
     */
    public String getTransformName() {
        return ""
    }

    /**
     * Return the task name(exclude transform task)
     */
    public String getTaskName() {
        return "${transformName}For${apkVariant.name.capitalize()}"
    }

    /**
     * Callback function before the hooked task executes
     * @param task Hooked task
     */
    public abstract void beforeTaskExecute(T task)
    /**
     * Callback function after the hooked task executes
     * @param task Hooked task
     */
    public abstract void afterTaskExecute(T task)
}