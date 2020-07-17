package com.dovar.dplugin

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.dovar.dplugin.hooker.TaskHookerManager
import com.dovar.dplugin.transform.StripClassAndResTransform
import com.dovar.dplugin.utils.FileBinaryCategory
import com.dovar.dplugin.utils.Log
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ResolutionStrategy
import org.gradle.internal.reflect.Instantiator
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry

import javax.inject.Inject

/**
 * DPlugin gradle plugin for plugin project
 */
class DPlugin extends BasePlugin {

    //Files be retained after host apk build
    //private def hostFileNames = ['versions', 'R.txt', 'mapping.txt', 'versions.txt', 'Host_R.txt'] as Set

    /**
     * Stores files generated by the host side and is used when building plugin apk
     */
    private def hostDir

    protected boolean isBuildingPlugin = false
    private boolean checked

    /**
     * TaskHooker manager, registers hookers when apply invoked
     */
    private TaskHookerManager taskHookerManager

    private StripClassAndResTransform stripClassAndResTransform

    @Inject
    public DPlugin(Instantiator instantiator, ToolingModelBuilderRegistry registry) {
        super(instantiator, registry)
    }

    @Override
    protected void beforeCreateAndroidTasks(boolean isBuildingPlugin) {
        this.isBuildingPlugin = isBuildingPlugin
        if (!isBuildingPlugin) {
            Log.i 'DPlugin', "Skipped all DPlugin configurations!"
            return
        }
        Log.i 'DPlugin', "DPlugin configurations start!"

        if (getDPluginConfig().mode == 0) {
            Log.i 'DPlugin', "getDPluginConfig().mode == 0"
            return
        }

        checkConfig()

        stripClassAndResTransform = new StripClassAndResTransform(project)
        android.registerTransform(stripClassAndResTransform)

        android.defaultConfig.buildConfigField("int", "PACKAGE_ID", "0x" + Integer.toHexString(DPluginConfig.packageId))

        HashSet<String> replacedSet = [] as HashSet
        project.rootProject.subprojects { Project p ->
//            Log.i 'Dependencies', "project.rootProject.subprojects: ${p.name}"
            p.configurations.all { Configuration configuration ->
                //--以下发生在config阶段 begin--//
                configuration.resolutionStrategy { ResolutionStrategy resolutionStrategy ->
//                    Log.i 'Dependencies', "configuration.resolutionStrategy: ${p.name}"
                    //--end--//
                    resolutionStrategy.eachDependency { DependencyResolveDetails details ->
                        if (!isBuildingPlugin) {
                            return
                        }
//                        Log.i 'Dependencies', "eachDependency: ${details.requested.group}:${details.requested.name}:${details.requested.version}  project:${p.name}"

                        checkConfig()

                        def hostDependency = DPluginConfig.hostDependencies.get("${details.requested.group}:${details.requested.name}")
                        if (hostDependency != null) {
                            if ("${details.requested.version}" != "${hostDependency['version']}") {
                                String key = "${p.name}:${details.requested}"
                                if (!replacedSet.contains(key)) {
                                    replacedSet.add(key)
                                    if (DPluginConfig.forceUseHostDependences) {
                                        Log.i 'Dependencies', "ATTENTION: Replaced module [${details.requested}] in project(:${p.name})'s configuration to host version: [${hostDependency['version']}]!"
                                    } else {
                                        DPluginConfig.addWarning "WARNING: [${details.requested}] in project(:${p.name})'s configuration will be occupied by Host App! Please change it to host version: [${hostDependency['group']}:${hostDependency['name']}:${hostDependency['version']}]."
                                        DPluginConfig.setFlag('tip.forceUseHostDependences', true)
                                    }
                                }

                                if (DPluginConfig.forceUseHostDependences) {
                                    details.useVersion(hostDependency['version'])
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    File getJarPath() {
        URL url = this.class.getResource("")
        int index = url.path.indexOf('!')
        if (index < 0) {
            index = url.path.length()
        }
        return project.file(url.path.substring(0, index))
    }

    @Override
    void apply(final Project project) {
        Object.apply(project)

        hostDir = new File(project.buildDir, "plugin/host")
        if (!hostDir.exists()) {
            hostDir.mkdirs()
        }

        DPluginConfig.hostDependenceFile = new File(hostDir, "versions.txt")

        project.afterEvaluate {
            if (!isBuildingPlugin) {
                return
            }
            if (getDPluginConfig().mode == 0) {
                Log.i 'DPlugin', "getDPluginConfig().mode == 0"
                taskHookerManager = new VASimpleTaskHookerManager(project, instantiator)
                taskHookerManager.registerTaskHookers()
                return
            }

            stripClassAndResTransform.onProjectAfterEvaluate()
            taskHookerManager = new VATaskHookerManager(project, instantiator)
            taskHookerManager.registerTaskHookers()

            if (android.dataBinding.enabled) {
                project.dependencies.add('annotationProcessor', project.files(jarPath.absolutePath))
            }

            android.applicationVariants.each { ApplicationVariantImpl variant ->

                DPluginConfig.with {
                    DExtention.VAContext vaContext = getVaContext(variant.name)
                    vaContext.packageName = variant.applicationId
                    //manifest跟applicationId未必一致，r文件使用的是manifest中的package
                    //所以这里先用applicationId设为默认值，后面在MergeManifestsHooker中对值进行修正
                    vaContext.packagePath = vaContext.packageName.replace('.'.charAt(0), File.separatorChar)
                    vaContext.hostSymbolFile = new File(hostDir, "Host_R.txt")
                }
            }
        }
    }

    /**
     * Check the plugin apk related config infos
     */
    private void checkConfig() {
        if (checked) {
            return
        }
        checked = true

        int packageId = DPluginConfig.packageId
        if (packageId == 0) {
            def err = new StringBuilder('you should set the packageId in build.gradle,\n ')
            err.append('please declare it in application project build.gradle:\n')
            err.append('    dpluginConfig {\n')
            err.append('        packageId = 0xXX \n')
            err.append('    }\n')
            err.append('apply for the value of packageId.\n')
            throw new InvalidUserDataException(err.toString())
        }
        if (packageId >= 0x7f || packageId <= 0x01) {
            throw new IllegalArgumentException('the packageId must be in [0x02, 0x7E].')
        }

        String targetHost = DPluginConfig.targetHost
        if (!targetHost) {
            def err = new StringBuilder('\nyou should specify the targetHost in build.gradle, e.g.: \n')
            err.append('    dpluginConfig {\n')
            err.append('        //when target Host in local machine, value is host application directory\n')
            err.append('        targetHost = ../xxxProject/app \n')
            err.append('    }\n')
            throw new InvalidUserDataException(err.toString())
        }

        File hostLocalDir = new File(targetHost)
        if (!hostLocalDir.exists()) {
            def err = "The directory of host application doesn't exist! Dir: ${hostLocalDir.canonicalPath}"
            throw new InvalidUserDataException(err)
        }

        File hostR = new File(hostLocalDir, "build/FAHost/Host_R.txt")
        if (hostR.exists()) {
            def dst = new File(hostDir, "Host_R.txt")
            use(FileBinaryCategory) {
                dst << hostR
            }
        } else {
            def err = new StringBuilder("Can't find ${hostR.canonicalPath}, please check up your host application\n")
            err.append("  need apply com.dovar.dplugin.host in build.gradle of host application\n ")
            throw new InvalidUserDataException(err.toString())
        }

        File hostVersions = new File(hostLocalDir, "build/FAHost/versions.txt")
        if (hostVersions.exists()) {
            def dst = new File(hostDir, "versions.txt")
            use(FileBinaryCategory) {
                dst << hostVersions
            }
        } else {
            def err = new StringBuilder("Can't find ${hostVersions.canonicalPath}, please check up your host application\n")
            err.append("  need apply com.dovar.dplugin.host in build.gradle of host application \n")
            throw new InvalidUserDataException(err.toString())
        }

        File hostMapping = new File(hostLocalDir, "build/FAHost/mapping.txt")
        if (hostMapping.exists()) {
            def dst = new File(hostDir, "mapping.txt")
            use(FileBinaryCategory) {
                dst << hostMapping
            }
        }
    }

    static class VATaskHookerManager extends TaskHookerManager {

        VATaskHookerManager(Project project, Instantiator instantiator) {
            super(project, instantiator)
        }

        @Override
        void registerTaskHookers() {
            android.applicationVariants.all { ApplicationVariantImpl appVariant ->
                if (!appVariant.buildType.name.equalsIgnoreCase("release")) {
                    return
                }

                registerTaskHooker(instantiator.newInstance(PrepareDependenciesHooker, project, appVariant))
                registerTaskHooker(instantiator.newInstance(MergeAssetsHooker, project, appVariant))
                registerTaskHooker(instantiator.newInstance(MergeManifestsHooker, project, appVariant))
                registerTaskHooker(instantiator.newInstance(MergeJniLibsHooker, project, appVariant))
                //不调用close()会导致resources.arsc没释放
                registerTaskHooker(instantiator.newInstance(ProcessResourcesHooker, project, appVariant))
                registerTaskHooker(instantiator.newInstance(ProguardHooker, project, appVariant))
                //用删减版的R.class替换原有R.class，非必须
                //gradle 3.4后useDexArchive一定启动，所以DxTaskHooker实际已经不生效了
//                registerTaskHooker(instantiator.newInstance(DxTaskHooker, project, appVariant))
            }
        }
    }

    static class VASimpleTaskHookerManager extends TaskHookerManager {

        VASimpleTaskHookerManager(Project project, Instantiator instantiator) {
            super(project, instantiator)
        }

        @Override
        void registerTaskHookers() {
            android.applicationVariants.all { ApplicationVariantImpl appVariant ->
                if (!appVariant.buildType.name.equalsIgnoreCase("release")) {
                    return
                }

                registerTaskHooker(instantiator.newInstance(SimpleManifestHooker, project, appVariant))
            }
        }
    }
}
