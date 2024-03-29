package com.dovar.dplugin.aapt

import com.dovar.dplugin.collector.res.PluginResourceEntry
import com.dovar.dplugin.collector.res.ResourceEntry
import com.dovar.dplugin.collector.res.StyleableEntry
import com.google.common.collect.ListMultimap
import com.google.common.io.Files
import groovy.io.FileType

/**
 * Class to expand aapt function
 */
public class Aapt {

    public static final int ID_DELETED = -1
    public static final int ID_NO_ATTR = -2

    public static final String RESOURCES_ARSC = 'resources.arsc'

    private static final String ENTRY_SEPARATOR = '/'

    private final File assetDir
    private final File symbolFile
    private final def toolsRevision

    Aapt(final File assetDir, File symbolFile, toolsRevision) {
        this.assetDir = assetDir
        this.symbolFile = symbolFile
        this.toolsRevision = toolsRevision
    }

    /**
     * Filter package assets by specific types
     *
     * @param retainedTypes
     *            the resource types to retain
     * @param pp
     *            new package id
     * @param idMaps
     */
    void filterPackage(final List<PluginResourceEntry> retainedTypes, final List<?> retainedStyleables, final int pp, final Map<?, ?> idMaps, final Map<?, ?> libRefTable, final Set<String> outUpdatedResources) {
        final File arscFile = new File(this.assetDir, RESOURCES_ARSC)
        final def arscEditor = new ArscEditor(arscFile, toolsRevision)

        // Filter R.txt
        if (this.symbolFile != null) {
            this.filterRTxt(this.symbolFile, retainedTypes, retainedStyleables)
        }

        arscEditor.slice(pp, idMaps, libRefTable, retainedTypes)
        outUpdatedResources.add(RESOURCES_ARSC)
        this.resetAllXmlPackageId(this.assetDir, pp, idMaps, outUpdatedResources)
    }


    /**
     * Filter resources with specific types
     *
     * @param retainedTypes
     */
    void filterResources(final List<?> retainedTypes, final Set<String> outFilteredResources) {
        def resDir = new File(assetDir, 'res')
        resDir.listFiles().each { typeDir ->
            def type = retainedTypes.find { typeDir.name.startsWith(it.name) }
            if (type == null) {
                typeDir.listFiles().each {
                    outFilteredResources.add("res/$typeDir.name/$it.name")
                }

                typeDir.deleteDir()
                return
            }

            def entryFiles = typeDir.listFiles()
            def retainedEntryCount = entryFiles.size()

            entryFiles.each { entryFile ->
                def entry = type.entries.find { entryFile.name.startsWith("${it.name}.") }
                if (entry == null) {
                    outFilteredResources.add("res/$typeDir.name/$entryFile.name")
                    entryFile.delete()
                    retainedEntryCount--
                }
            }

            if (retainedEntryCount == 0) {
                typeDir.deleteDir()
            }
        }
    }


    /**
     * Reset package id for *.xml
     */
    private static void resetAllXmlPackageId(final File dir, final int pp, final Map<?, ?> idMaps, final Set<String> outUpdatedResources) {
        int len = dir.canonicalPath.length() + 1 // bypass '/'
        def isWindows = (File.separator != ENTRY_SEPARATOR)

        dir.eachFileRecurse(FileType.FILES) { file ->
            if ('xml'.equalsIgnoreCase(Files.getFileExtension(file.name))) {
                new AXmlEditor(file).setPackageId(pp, idMaps)

                if (outUpdatedResources != null) {
                    def path = file.canonicalPath.substring(len)
                    if (isWindows) { // compat for windows
                        path = path.replaceAll('\\\\', ENTRY_SEPARATOR)
                    }

                    outUpdatedResources.add(path)
                }
            }
        }
    }

    public static void generateRJava(File dest, String pkg, ListMultimap<String, ResourceEntry> resources, List<StyleableEntry> styleables) {
        if (!dest.parentFile.exists()) {
            dest.parentFile.mkdirs()
        }

        if (!dest.exists()) {
            dest.createNewFile()
        }

        dest.withPrintWriter { pw ->
            pw.println "/* AUTO-GENERATED FILE.  DO NOT MODIFY."
            pw.println " * "
            pw.println " * This class was automatically generated by the"
            pw.println " * aapt tool from the resource data it found.  It"
            pw.println " * should not be modified by hand."
            pw.println " */"
            pw.println "package ${pkg};"
            pw.println "public final class R {"

            resources.keySet().each { type ->
                pw.println "    public static final class ${type} {"
                resources.get(type).each { entry ->
                    pw.println "        public static final int ${entry.resourceName} = ${entry.hexNewResourceId};"
                }
                pw.println "    }"
            }

            pw.println "    public static final class styleable {"
            styleables.each { styleable ->
                    pw.println "        public static final ${styleable.valueType} ${styleable.name} = ${styleable.value};"
            }
            pw.println "    }"
            pw.println "}"
        }
    }


    /**
     * Filter specify types for R.txt
     *
     * @param rTxt
     *            The R.txt
     * @param retainedTypes
     */
    private static def filterRTxt(final File rTxt, final List<PluginResourceEntry> retainedTypes, final List<?> retainedStyleables) {
        rTxt.write('')
        rTxt.withPrintWriter { pw ->
            retainedTypes.each { t ->
                t.entries.each { e ->
                    pw.println("${t.type} ${t.name} ${e.name} ${e._vs}")
                }
            }
            retainedStyleables.each {
                pw.println("${it.vtype} ${it.type} ${it.key} ${it.idStr}")
            }
        }
    }
}
