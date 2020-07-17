package com.dovar.dplugin.hooker


import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.ide.dependencies.BuildMappingUtils
import com.android.build.gradle.internal.ide.dependencies.DependencyGraphBuilderKt
import com.android.build.gradle.internal.tasks.AppPreBuildTask
import com.android.builder.model.Dependencies
import com.android.builder.model.SyncIssue
import com.dovar.dplugin.collector.dependence.AarDependenceInfo
import com.dovar.dplugin.collector.dependence.DependenceInfo
import com.dovar.dplugin.collector.dependence.JarDependenceInfo
import com.dovar.dplugin.utils.FileUtil
import com.dovar.dplugin.utils.Log
import com.google.common.collect.ImmutableMap
import org.gradle.api.Project

import java.util.function.Consumer

/**
 * Gather list of dependencies(aar&jar) need to be stripped&retained after the PrepareDependenciesTask finished.
 * The entire stripped operation throughout the build lifecycle is based on the result of this hooker。
 *
 * 配置stripDependencies，用于之后从插件中去掉与宿主重复的依赖库
 */
class PrepareDependenciesHooker extends GradleTaskHooker<AppPreBuildTask> {

    //group:artifact:version
    def hostDependencies = [] as Set

    def retainedAarLibs = [] as Set<AarDependenceInfo>
    def retainedJarLib = [] as Set<JarDependenceInfo>
    def stripDependencies = [] as Collection<DependenceInfo>

    public PrepareDependenciesHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }

    @Override
    String getTaskName() {
        return scope.getTaskName('pre', 'Build')
    }

    /**
     * Collect host dependencies via hostDependenceFile or exclude configuration before PrepareDependenciesTask execute,
     * @param task Gradle Task fo PrepareDependenciesTask
     */
    @Override
    void beforeTaskExecute(AppPreBuildTask task) {
        hostDependencies.addAll(pluginConfig.hostDependencies.keySet())
        pluginConfig.excludes.each { String artifact ->
            final def module = artifact.split(':')
            hostDependencies.add("${module[0]}:${module[1]}")
        }
    }

    /**
     * Classify all dependencies into retainedAarLibs & retainedJarLib & stripDependencies
     *
     * @param task Gradle Task fo PrepareDependenciesTask
     */
    @Override
    void afterTaskExecute(AppPreBuildTask task) {
        Consumer consumer = new Consumer<SyncIssue>() {
            @Override
            void accept(SyncIssue syncIssue) {
                Log.i 'PrepareDependenciesHooker', "Error: ${syncIssue}"
            }
        }
        ImmutableMap<String, String> buildMapping = BuildMappingUtils.computeBuildMapping(project.gradle)
        Log.i 'PrepareDependenciesHooker', "createDependencies"
        //如果项目开启了并行编译，这里将会出现无限等待
        Dependencies dependencies = DependencyGraphBuilderKt.getDependencyGraphBuilder().createDependencies(scope, false, buildMapping, consumer)
        Log.i 'PrepareDependenciesHooker', "createDependencies end"

        dependencies.libraries.each {
            def mavenCoordinates = it.resolvedCoordinates
            String group = mavenCoordinates.groupId
            if (group == null || group.length() == 0) {
                group = "unspecified"
            }
            if (hostDependencies.contains("${group}:${mavenCoordinates.artifactId}")) {
//                Log.i 'PrepareDependenciesHooker', "Need strip aar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                //Gradle3.4.2时ProjectComponentIdentifier的groupId会是 artifacts，后期升级可能需要适配
                //只对 stripDependencies 里的依赖做这个区分
                stripDependencies.add(
                        new AarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it, "artifacts" == group))

            } else {
//                Log.i 'PrepareDependenciesHooker', "Need retain aar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                retainedAarLibs.add(
                        new AarDependenceInfo(
                                mavenCoordinates.groupId,
                                mavenCoordinates.artifactId,
                                mavenCoordinates.version,
                                it))
            }
        }
        dependencies.javaLibraries.each {
            def mavenCoordinates = it.resolvedCoordinates
            if (mavenCoordinates.artifactId.endsWith('.jar')) {
                //本地的jar包，示例：__local_aars__:/Users/xxx/libs/demo-1.0.0.jar
                //不用看version和groupId
                int lastSeparatorIndex = mavenCoordinates.artifactId.lastIndexOf(File.separator)

                String jar = mavenCoordinates.artifactId
                if (lastSeparatorIndex > 0) {
                    jar = jar.substring(lastSeparatorIndex + 1)
                }
                if (hostDependencies.contains("unspecified:${jar}")) {
//                    Log.i 'PrepareDependenciesHooker', "Need strip jar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                    stripDependencies.add(
                            new JarDependenceInfo(
                                    mavenCoordinates.groupId,
                                    mavenCoordinates.artifactId,
                                    mavenCoordinates.version,
                                    it))
                } else {
//                    Log.i 'PrepareDependenciesHooker', "Need retain jar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                    retainedJarLib.add(
                            new JarDependenceInfo(
                                    mavenCoordinates.groupId,
                                    mavenCoordinates.artifactId,
                                    mavenCoordinates.version,
                                    it))
                }
            } else {
                String group = mavenCoordinates.groupId
                if (group == null || group.length() == 0) {
                    group = "unspecified"
                }
                if (hostDependencies.contains("${group}:${mavenCoordinates.artifactId}")) {
//                    Log.i 'PrepareDependenciesHooker', "Need strip jar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                    stripDependencies.add(
                            new JarDependenceInfo(
                                    mavenCoordinates.groupId,
                                    mavenCoordinates.artifactId,
                                    mavenCoordinates.version,
                                    it))
                } else {
//                    Log.i 'PrepareDependenciesHooker', "Need retain jar: ${mavenCoordinates.groupId}:${mavenCoordinates.artifactId}:${mavenCoordinates.version}"
                    retainedJarLib.add(
                            new JarDependenceInfo(
                                    mavenCoordinates.groupId,
                                    mavenCoordinates.artifactId,
                                    mavenCoordinates.version,
                                    it))
                }
            }
        }

        File hostDir = vaContext.getBuildDir(scope)
        FileUtil.saveFile(hostDir, "${taskName}-stripDependencies", stripDependencies)
        FileUtil.saveFile(hostDir, "${taskName}-retainedAarLibs", retainedAarLibs)
        FileUtil.saveFile(hostDir, "${taskName}-retainedJarLib", retainedJarLib)

        checkDependencies()

        Log.i 'PrepareDependenciesHooker', "Analyzed all dependencis. Get more infomation in dir: ${hostDir.absoluteFile}"

        vaContext.stripDependencies = stripDependencies
        vaContext.retainedAarLibs = retainedAarLibs
        mark()
    }

    //如果插件要保留support和databinding，则提示必须在宿主中引入，不允许插件单独引入
    void checkDependencies() {
        ArrayList<DependenceInfo> allRetainedDependencies = new ArrayList<>()
        allRetainedDependencies.addAll(retainedAarLibs)
        allRetainedDependencies.addAll(retainedJarLib)

        ArrayList<String> checked = new ArrayList<>()

        allRetainedDependencies.each {
            String group = it.group
            String artifact = it.artifact
            String version = it.version

            // com.android.support:all
            if (group == 'com.android.support' || group.startsWith('com.android.support.')) {
                checked.add("${group}:${artifact}:${version}")
            }

            // com.android.databinding:all
            if (group == 'com.android.databinding' || group.startsWith('com.android.databinding.')) {
                checked.add("${group}:${artifact}:${version}")
            }
        }

        if (!checked.empty) {
            throw new Exception("The dependencies [${String.join(', ', checked)}] that will be used in the current plugin must be included in the host app first. Please add it in the host app as well.")
        }
    }
}