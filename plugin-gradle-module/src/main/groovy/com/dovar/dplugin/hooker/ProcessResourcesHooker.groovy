package com.dovar.dplugin.hooker

import com.android.build.gradle.AndroidConfig
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.internal.scope.ExistingBuildElements
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.tasks.ProcessAndroidResources
import com.android.sdklib.BuildToolInfo
import com.dovar.dplugin.aapt.Aapt
import com.dovar.dplugin.collector.ResourceCollector
import com.dovar.dplugin.collector.res.PluginResourceEntry
import com.dovar.dplugin.collector.res.PluginResourceSubEntry
import com.dovar.dplugin.collector.res.ResourceEntry
import com.dovar.dplugin.collector.res.StyleableEntry
import com.dovar.dplugin.utils.FileUtil
import com.dovar.dplugin.utils.Log
import com.dovar.dplugin.utils.ZipUtil
import com.google.common.collect.ListMultimap
import com.google.common.io.Files
import org.gradle.api.Project

/**
 * Filter the host resources out of the plugin apk.
 * Modify the .arsc file to delete host element,
 * rearrange plugin element, hold the new resource IDs
 */
class ProcessResourcesHooker extends GradleTaskHooker<ProcessAndroidResources> {

    /**
     * Collector to gather the sources and styleables
     */
    ResourceCollector resourceCollector
    /**
     * Android config information specified in build.gradle
     */
    AndroidConfig androidConfig

    ProcessResourcesHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
        androidConfig = project.extensions.findByType(AppExtension)
    }

    @Override
    String getTaskName() {
        return scope.getTaskName('process', 'Resources')
    }

    @Override
    void beforeTaskExecute(ProcessAndroidResources aaptTask) {

    }

    /**
     * Since we need to remove the host resources and modify the resource ID,
     * we will reedit the AP_ file and repackage it after the task execute
     *
     * @param par Gradle task of process android resources
     */
    @Override
    void afterTaskExecute(ProcessAndroidResources par) {
        File outputFile = ExistingBuildElements.from(InternalArtifactType.PROCESSED_RES, scope.getArtifacts().getFinalArtifactFiles(InternalArtifactType.PROCESSED_RES))
                .element(variantData.outputScope.mainSplit)
                .getOutputFile()

        repackage(par, outputFile)
    }

    void repackage(ProcessAndroidResources par, File apFile) {
        def resourcesDir = new File(apFile.parentFile, Files.getNameWithoutExtension(apFile.name))

        /*
         * Clean up resources merge directory
         */
        resourcesDir.deleteDir()

        File backupFile = new File(apFile.getParentFile(), "${Files.getNameWithoutExtension(apFile.name)}-original.${Files.getFileExtension(apFile.name)}")
        backupFile.delete()
        project.copy {
            from apFile
            into apFile.getParentFile()
            rename { backupFile.name }
        }

        Log.i 'repackage', "backupFile: ${backupFile.name}"

        /*
         * Unzip resources-${variant.name}.ap_
         */
        project.copy {
            from project.zipTree(apFile)
            into resourcesDir

            include 'AndroidManifest.xml'
            include 'resources.arsc'
            include 'res/**/*'
        }

        resourceCollector = new ResourceCollector(project, par)
        resourceCollector.collect()

        List<PluginResourceEntry> retainedTypes = convertResourcesForAapt(resourceCollector.pluginResources)
        def retainedStylealbes = convertStyleablesForAapt(resourceCollector.pluginStyleables)
        def resIdMap = resourceCollector.resIdMap

        def rSymbolFile = par.textSymbolOutputFile
        def libRefTable = ["${pluginConfig.packageId}": par.applicationId]
        def filteredResources = [] as HashSet<String>
        def updatedResources = [] as HashSet<String>

        def aapt = new Aapt(resourcesDir, rSymbolFile, androidConfig.buildToolsRevision)

        //Delete host resources, must do it before filterPackage
        aapt.filterResources(retainedTypes, filteredResources)

        //Modify the arsc file, and replace ids of related xml files
        aapt.filterPackage(retainedTypes, retainedStylealbes, pluginConfig.packageId, resIdMap, libRefTable, updatedResources)


        File hostDir = vaContext.getBuildDir(scope)
        FileUtil.saveFile(hostDir, "${taskName}-retainedTypes", retainedTypes)
        FileUtil.saveFile(hostDir, "${taskName}-retainedStylealbes", retainedStylealbes)
        FileUtil.saveFile(hostDir, "${taskName}-filteredResources", true, filteredResources)
        FileUtil.saveFile(hostDir, "${taskName}-updatedResources", true, updatedResources)

        /*
         * Delete filtered entries and then add updated resources into resources-${variant.name}.ap_
         */
        ZipUtil.with(apFile).deleteAll(filteredResources + updatedResources)

        project.exec {
            executable par.buildTools.getPath(BuildToolInfo.PathId.AAPT)
            workingDir resourcesDir
            args 'add', apFile.path
            args updatedResources
            standardOutput = System.out
            errorOutput = System.err
        }

        updateRJava(aapt, par.sourceOutputDir)
        mark()
    }

    /**
     * Because the resource ID has changed, we need to regenerate the R.java file,
     * include the all resources R, plugin resources R, and R files of retained aars
     *
     * @param aapt Class to expand aapt function
     * @param sourceOutputDir Directory of R.java files generated by aapt
     *
     */
    def updateRJava(Aapt aapt, File sourceOutputDir) {
        File vaBuildDir = vaContext.getBuildDir(scope)
        File backupDir = new File(vaBuildDir, "origin/r")

        project.ant.move(todir: backupDir) {
            fileset(dir: sourceOutputDir) {
                include(name: '**/R.java')
            }
        }

        FileUtil.deleteEmptySubfolders(sourceOutputDir)

        def rSourceFile = new File(sourceOutputDir, "${vaContext.packagePath}${File.separator}R.java")
        aapt.generateRJava(rSourceFile, vaContext.packagePath.replace(File.separatorChar, '.'.charAt(0)), resourceCollector.allResources, resourceCollector.allStyleables)
//        Log.i 'ProcessResourcesHooker', "Updated R.java: ${rSourceFile.absoluteFile}"

        def splitRSourceFile = new File(vaBuildDir, "source${File.separator}r${File.separator}${vaContext.packagePath}${File.separator}R.java")
        aapt.generateRJava(splitRSourceFile, vaContext.packagePath.replace(File.separatorChar, '.'.charAt(0)), resourceCollector.pluginResources, resourceCollector.pluginStyleables)
//        Log.i 'ProcessResourcesHooker', "Updated R.java: ${splitRSourceFile.absoluteFile}"
        vaContext.splitRJavaFile = splitRSourceFile

        vaContext.retainedAarLibs.each {
            def aarPackage = it.package
            def rJavaFile = new File(sourceOutputDir, "${aarPackage.replace('.'.charAt(0), File.separatorChar)}${File.separator}R.java")
            aapt.generateRJava(rJavaFile, aarPackage, it.aarResources, it.aarStyleables)
//            Log.i 'ProcessResourcesHooker', "Updated R.java: ${rJavaFile.absoluteFile}"
        }
    }

    /**
     * We use the third party library to modify the ASRC file,
     * this method used to transform resource data into the structure of the library
     * @param pluginResources Map of plugin resources
     */
    def convertResourcesForAapt(ListMultimap<String, ResourceEntry> pluginResources) {
        def retainedTypes = []

        pluginResources.keySet().each { resType ->
            def firstEntry = pluginResources.get(resType).get(0)

            def typeEntry = new PluginResourceEntry("int", resType, parseTypeIdFromResId(firstEntry.resourceId), parseTypeIdFromResId(firstEntry.newResourceId), [])
            pluginResources.get(resType).each { resEntry ->
                def sub = new PluginResourceSubEntry(resEntry.resourceName,
                        parseEntryIdFromResId(resEntry.resourceId),
                        parseEntryIdFromResId(resEntry.newResourceId),
                        resEntry.resourceId,
                        resEntry.newResourceId,
                        resEntry.hexResourceId,
                        resEntry.hexNewResourceId)
                typeEntry.entries.add(sub)
            }

            retainedTypes.add(typeEntry)
        }

        retainedTypes.sort { t1, t2 ->
            t1._id - t2._id
        }

        return retainedTypes
    }

    /**
     * Transform styleable data into the structure of the aapt library
     * @param pluginStyleables Map of plugin styleables
     */
    def convertStyleablesForAapt(List<StyleableEntry> pluginStyleables) {
        def retainedStyleables = []
        pluginStyleables.each { styleableEntry ->
            retainedStyleables.add([vtype: styleableEntry.valueType,
                                    type : 'styleable',
                                    key  : styleableEntry.name,
                                    idStr: styleableEntry.value])
        }
        return retainedStyleables
    }

    /**
     * Parse the type part of a android resource id
     */
    def parseTypeIdFromResId(int resourceId) {
        resourceId >> 16 & 0xFF
    }

    /**
     * Parse the entry part of a android resource id
     */
    def parseEntryIdFromResId(int resourceId) {
        resourceId & 0xFFFF
    }

}