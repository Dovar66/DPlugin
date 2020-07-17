package com.dovar.dplugin.collector.dependence

import com.android.SdkConstants
import com.android.build.gradle.internal.TaskManager
import com.android.builder.model.AndroidLibrary
import com.android.utils.FileUtils
import com.dovar.dplugin.collector.res.ResourceEntry
import com.dovar.dplugin.collector.res.StyleableEntry
import com.dovar.dplugin.utils.Log
import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.ListMultimap
import com.google.common.collect.Lists

/**
 * Represents a AAR dependence from Maven repository or Android library module
 */
class AarDependenceInfo extends DependenceInfo {

    /**
     * Android library dependence in android build system, delegate of AarDependenceInfo
     */
    AndroidLibrary library

    File intermediatesFile

    /**
     * 标记是否是源码依赖的module
     *
     * 只对 stripDependencies 里的依赖做这个区分
     */
    boolean isSourceModule

    /**
     * All resources(e.g. drawable, layout...) this library can access
     * include resources of self-project and dependence(direct&transitive) project
     */
    ListMultimap<String, ResourceEntry> aarResources = ArrayListMultimap.create()
    /**
     * All styleables this library can access, like "aarResources"
     */
    List<StyleableEntry> aarStyleables = Lists.newArrayList()

    AarDependenceInfo(String group, String artifact, String version, AndroidLibrary library) {
        this(group, artifact, version, library, false)
    }

    AarDependenceInfo(String group, String artifact, String version, AndroidLibrary library, boolean isSourceModule) {
        super(group, artifact, version)
        this.library = library
        this.isSourceModule = isSourceModule
    }

    @Override
    File getJarFile() {
        if (isSourceModule) {
            String jarFile = library.jarFile.getAbsolutePath()
            String intermediates = jarFile.substring(0, jarFile.indexOf('intermediates') + 13 + 1)
            String dest = intermediates + File.separator + "compile_library_classes" + File.separator + "release" + File.separator + "classes.jar"
//            Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s jar file: ${dest}"
            return new File(dest)
        }
//        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s jar file: ${library.jarFile}"
        return library.jarFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.AAR
    }

    File getAssetsFolder() {
//        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s assets folder: ${library.assetsFolder}"
        return library.assetsFolder
    }

    File getJniFolder() {
//        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s jni folder: ${library.jniFolder}"
        return library.jniFolder
    }

    Collection<File> getLocalJars() {
//        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s local jars: ${library.localJars}"
        if (isSourceModule && library.localJars != null && library.localJars.size() > 0) {
            throw new RuntimeException("SourceModule 尚未适配 localJars!")
        }
        return library.localJars
    }

    /**
     * Return collection of "resourceType:resourceName", parse from R symbol file
     * @return set of a combination of resource type and name
     */
    public Set<String> getResourceKeys() {

        def resKeys = [] as Set<String>

        def rSymbol = getFile(library.symbolFile, TaskManager.DIR_BUNDLES, library.projectVariant, SdkConstants.FN_RESOURCE_TEXT)
        if (rSymbol.exists()) {
//            Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s symbol file: ${rSymbol}"
            rSymbol.eachLine { line ->
                if (!line.empty) {
                    def tokenizer = new StringTokenizer(line)
                    def valueType = tokenizer.nextToken()
                    def resType = tokenizer.nextToken()
                    // resource type (attr/string/color etc.)
                    def resName = tokenizer.nextToken()       // resource name

                    resKeys.add("${resType}:${resName}")
                }
            }
        }

        return resKeys
    }

    /**
     * Return the package name of this library, parse from manifest file
     * manifest file are obtained by delegating to "library"
     * @return package name of this library
     */
    public String getPackage() {
        File manifest = getFile(library.manifest, 'library_manifest', library.projectVariant, SdkConstants.ANDROID_MANIFEST_XML)
//        Log.i 'AarDependenceInfo', "Found [${library.resolvedCoordinates}]'s manifest file: ${manifest}"
        def xmlManifest = new XmlParser().parse(manifest)
        return xmlManifest.@package
    }

    File getIntermediatesDir() {
        if (intermediatesFile == null) {
            String path = library.folder.path
            try {
                intermediatesFile = new File(path.substring(0, path.indexOf("${File.separator}intermediates${File.separator}")), 'intermediates')

            } catch (Exception e) {
                Log.e('AarDependenceInfo', "Can not find [${library.resolvedCoordinates}]'s intermediates dir from the path: ${path}")
                intermediatesFile = library.folder
            }
        }
        return intermediatesFile
    }

    File getFile(File defaultFile, String... paths) {
        if (library.projectVariant == null) {
            return defaultFile
        }

        if (defaultFile.exists()) {
            return defaultFile
        }

        // module library
        return FileUtils.join(intermediatesDir, paths)
    }

    @Override
    String toString() {
        return "${super.toString()} -> ${library}"
    }
}