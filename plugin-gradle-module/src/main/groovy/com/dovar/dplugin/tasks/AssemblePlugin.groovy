package com.dovar.dplugin.tasks

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.dovar.dplugin.DExtention
import com.dovar.dplugin.utils.Log
import com.sun.istack.internal.NotNull
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Gradle task for assemble plugin apk
 */
public class AssemblePlugin extends DefaultTask {

    @OutputDirectory
    File pluginApkDir

    @Input
    String appPackageName

    @Input
    String apkTimestamp

    @Input
    File originApkFile

    String variantName

    String buildDir

    /**
     * Copy the plugin apk to out/plugin directory and rename to
     * the format required for the backend system
     */
    @TaskAction
    public void outputPluginApk() {
        DExtention sDExtention = project.dpluginConfig
        sDExtention.getVaContext(variantName).checkList.check()
        sDExtention.printWarning(name)

        if (sDExtention.getFlag('tip.forceUseHostDependences')) {
            def tip = new StringBuilder('To avoid configuration WARNINGs, you could set the forceUseHostDependences to be true in build.gradle,\n ')
            tip.append('please declare it in application project build.gradle:\n')
            tip.append('    dpluginConfig {\n')
            tip.append('        forceUseHostDependences = true \n')
            tip.append('    }\n')
            Log.i name, tip.toString()
        }

        Log.i name, "More building infomation could be found in the dir: ${buildDir}."

        if (sDExtention.output != null && !sDExtention.output.isEmpty()) {
            pluginApkDir = new File(sDExtention.output)
            File file_host = new File(project.buildDir, "plugin/host")
            if (file_host.exists()) {
                //把其他文件也输出到指定目录下
                getProject().copy {
                    from file_host
                    into pluginApkDir
                }
            }
        }

        getProject().copy {
            from originApkFile
            into pluginApkDir

            def name = sDExtention.pluginName
            if (name == null || name.isEmpty()) {
                name = appPackageName
            }
            rename { "${name}_${sDExtention.pluginVersion}.plugin" }
        }
    }


    public static class ConfigAction implements Action<AssemblePlugin> {

        @NotNull
        Project project
        @NotNull
        ApplicationVariantImpl variant

        ConfigAction(@NotNull Project project, @NotNull ApkVariant variant) {
            this.project = project
            this.variant = variant
        }

        @Override
        void execute(AssemblePlugin assemblePluginTask) {
            DExtention sDExtention = project.dpluginConfig

            assemblePluginTask.appPackageName = variant.applicationId
            assemblePluginTask.apkTimestamp = new Date().format("yyyyMMddHHmmss")
            assemblePluginTask.originApkFile = variant.outputs[0].outputFile
            assemblePluginTask.pluginApkDir = new File(project.buildDir, "/plugin/${variant.name}")
            assemblePluginTask.variantName = variant.name
            assemblePluginTask.buildDir = sDExtention.getVaContext(variant.name).getBuildDir(variant.variantData.scope).canonicalPath

            assemblePluginTask.setGroup("build")
            assemblePluginTask.setDescription("Build ${variant.name.capitalize()} plugin apk")
            assemblePluginTask.dependsOn(variant.assemble.name)
        }
    }

}
