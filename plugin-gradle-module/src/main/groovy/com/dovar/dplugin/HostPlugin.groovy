package com.dovar.dplugin

import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.ide.dependencies.ArtifactUtils
import com.android.build.gradle.internal.ide.dependencies.BuildMappingUtils
import com.android.build.gradle.internal.ide.dependencies.ResolvedArtifact
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.transforms.ProGuardTransform
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.dovar.dplugin.utils.FileUtil
import com.dovar.dplugin.utils.Log
import com.google.common.collect.ImmutableMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier

/**
 * gradle plugin for host project,
 * The primary role of this class is to save the
 * information needed to build the plugin apk.
 */
public class HostPlugin implements Plugin<Project> {

    public static final String TAG = 'VAHostPlugin'
    Project project
    File vaHostDir

    @Override
    public void apply(Project project) {
        this.project = project

        //The target project must be a android application module
        if (!project.plugins.hasPlugin('com.android.application')) {
            Log.e(TAG, "application required!")
            return
        }

        vaHostDir = new File(project.getBuildDir(), "FAHost")

        project.afterEvaluate {
            project.android.applicationVariants.each { ApplicationVariantImpl variant ->
                Log.i TAG, "ApplicationVariantImpl: ${variant.name}"
                generateDependencies(variant)
                backupHostR(variant)
                backupProguardMapping(variant)
            }
        }
    }

    /**
     * Generate ${project.buildDir}/FAHost/versions.txt
     */
    def generateDependencies(ApplicationVariantImpl applicationVariant) {
        //强制每次都触发，不然存在cache时这个task会不执行
        applicationVariant.javaCompileProvider.get().outputs.upToDateWhen { false }
        applicationVariant.javaCompileProvider.get().doLast {
            FileUtil.saveFile(vaHostDir, "versions", {
                List<String> deps = new ArrayList<String>()
                Set<ResolvedArtifact> compileArtifacts
                ImmutableMap<String, String> buildMapping = BuildMappingUtils.computeBuildMapping(project.gradle)
                compileArtifacts = ArtifactUtils.getAllArtifacts(applicationVariant.variantData.scope, AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH, null, buildMapping)
                compileArtifacts.each { ResolvedArtifact artifact ->
                    ComponentIdentifier id = artifact.componentIdentifier
                    if (id instanceof ProjectComponentIdentifier) {
                        //源码依赖的module把group设为 artifacts,对应PrepareDependenciesHooker中的逻辑
                        deps.add("artifacts:${id.projectPath}:unspecified ${artifact.artifactFile.length()}")

                    } else if (id instanceof ModuleComponentIdentifier) {
                        String group = id.group
                        if (group == null || group.length() == 0) {
                            group = "unspecified"
                        }
                        String version = id.version
                        if (version == null || version.length() == 0) {
                            version = "unspecified"
                        }
                        deps.add("${group}:${id.module}:${version} ${artifact.artifactFile.length()}")

                    } else {
                        deps.add("unspecified:${id.displayName}:unspecified ${artifact.artifactFile.length()}")
                    }
                }

                Collections.sort(deps)
                return deps
            })
        }

    }

    /**
     * Save R symbol file
     */
    def backupHostR(ApplicationVariant applicationVariant) {
        final ProcessAndroidResources aaptTask = this.project.tasks["process${applicationVariant.name.capitalize()}Resources"]
        //强制每次都触发，不然存在cache时这个task会不执行
        aaptTask.outputs.upToDateWhen { false }
        aaptTask.doLast {
            Log.i TAG, "Save R symbol file: process${applicationVariant.name.capitalize()}Resources"
            project.copy {
                Log.i(TAG, "path:${aaptTask.textSymbolOutputFile.absolutePath}")
                from aaptTask.textSymbolOutputFile
                into vaHostDir
                rename { "Host_R.txt" }
            }
        }
    }

    /**
     * Save proguard mapping
     */
    def backupProguardMapping(ApplicationVariant applicationVariant) {
        if (applicationVariant.buildType.minifyEnabled) {
            TransformTask proguardTask = project.tasks["transformClassesAndResourcesWithProguardFor${applicationVariant.name.capitalize()}"]

            ProGuardTransform proguardTransform = proguardTask.transform
            File mappingFile = proguardTransform.mappingFile

            proguardTask.doLast {
                project.copy {
                    from mappingFile
                    into vaHostDir
                }
            }
        }
    }
}
