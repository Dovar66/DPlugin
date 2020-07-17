package com.dovar.dplugin.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.tasks.ManifestProcessorTask
import com.dovar.dplugin.utils.Log
import groovy.xml.QName
import groovy.xml.XmlUtil
import org.gradle.api.Project

/**
 * Created by weizonghe on 2020/6/8.
 */
class SimpleManifestHooker extends GradleTaskHooker<ManifestProcessorTask> {
    public static final String ANDROID_NAMESPACE = 'http://schemas.android.com/apk/res/android'

    SimpleManifestHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }

    @Override
    String getTaskName() {
        return scope.getTaskName('process', 'Manifest')
    }

    @Override
    void beforeTaskExecute(ManifestProcessorTask task) {
    }

    /**
     * Filter specific attributes from <application /> element after MergeManifests task executed
     */
    @Override
    void afterTaskExecute(ManifestProcessorTask task) {
        //删掉合成后的AndroidManifest中的icon、label等标签,必需
        File manifest = new File(task.manifestOutputDirectory.get().asFile, "AndroidManifest.xml")
        if (manifest.exists()) {
            rewrite(manifest)
        } else {
            Log.i("MergeManifestsHooker", "afterTaskExecute manifest not exists: ${manifest.absolutePath}")
        }
    }

    static void rewrite(File xml) {
        if (xml?.exists()) {
            final Node manifest = new XmlParser().parse(xml)

            manifest.application.each {
                application ->
                    ['icon', 'label', 'allowBackup', 'supportsRtl'].each {
                        application.attributes().remove(new QName(ANDROID_NAMESPACE, it))
                    }
            }

            xml.withPrintWriter('utf-8', { pw ->
                XmlUtil.serialize(manifest, pw)
            })
        }
    }
}
