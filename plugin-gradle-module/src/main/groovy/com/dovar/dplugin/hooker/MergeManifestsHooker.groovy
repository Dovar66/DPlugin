package com.dovar.dplugin.hooker

import com.android.build.gradle.api.ApkVariant
import com.android.build.gradle.tasks.ManifestProcessorTask
import com.android.build.gradle.tasks.ProcessApplicationManifest
import com.dovar.dplugin.collector.dependence.DependenceInfo
import com.dovar.dplugin.utils.Log
import com.dovar.dplugin.utils.Reflect
import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.AbstractFileCollection
import org.gradle.api.tasks.TaskDependency

import java.util.function.Consumer
import java.util.function.Predicate

/**
 * Filter the stripped ManifestDependency in the ManifestDependency list of MergeManifests task
 *
 * ManifestProcessorTask在3.4.2上分为：ProcessApplicationManifest、ProcessLibraryManifest、ProcessTestManifest，其中ProcessLibraryManifest没有manifests变量。
 * 而3.2.1、3.0.0这些版本上只有MergeManifests。
 * 是否需要过滤掉ProcessLibraryManifest?
 */
class MergeManifestsHooker extends SimpleManifestHooker {


    MergeManifestsHooker(Project project, ApkVariant apkVariant) {
        super(project, apkVariant)
    }

    @Override
    void beforeTaskExecute(ManifestProcessorTask task) {
        if (task instanceof ProcessApplicationManifest) {
            //manifest跟applicationId不一致时，确保取的是manifest中的package
            //manifest被合并后package会被改成task.packageOverride
            def manifest = new XmlSlurper().parse(task.mainManifest)
            String pkg = manifest.@package
            Log.i("MergeManifestsHooker", "beforeTaskExecute ProcessApplicationManifest: ${task.name} ${pkg}")
            vaContext.packagePath = pkg.replace('.'.charAt(0), File.separatorChar)
        } else {
            Log.i("MergeManifestsHooker", "beforeTaskExecute not ProcessApplicationManifest: ${task.name}")
        }
        def stripAarNames = vaContext.stripDependencies.
                findAll {
                    it.dependenceType == DependenceInfo.DependenceType.AAR
                }.
                collect { DependenceInfo dep ->
                    "${dep.group}:${dep.artifact}:${dep.version}"
                } as Set<String>

        Reflect reflect = Reflect.on(task)
        ArtifactCollection manifests = new FixedArtifactCollection(this, reflect.get('manifests'), stripAarNames)
        reflect.set('manifests', manifests)
    }

    private static class FixedArtifactCollection implements ArtifactCollection {

        private MergeManifestsHooker hooker
        private ArtifactCollection origin
        def stripAarNames

        FixedArtifactCollection(MergeManifestsHooker hooker, ArtifactCollection origin, stripAarNames) {
            this.hooker = hooker
            this.origin = origin
            this.stripAarNames = stripAarNames
        }

        @Override
        FileCollection getArtifactFiles() {
            Set<File> set = getArtifacts().collect { ResolvedArtifactResult result ->
                result.file
            } as Set<File>
            FileCollection fileCollection = origin.getArtifactFiles()

            return new AbstractFileCollection() {
                @Override
                String getDisplayName() {
                    return fileCollection.getDisplayName()
                }

                @Override
                TaskDependency getBuildDependencies() {
                    return fileCollection.getBuildDependencies()
                }

                @Override
                Set<File> getFiles() {
                    Set<File> files = new LinkedHashSet(fileCollection.getFiles())
                    files.retainAll(set)
                    return files
                }
            }
        }

        @Override
        Set<ResolvedArtifactResult> getArtifacts() {
            Set<ResolvedArtifactResult> set = origin.getArtifacts()
            set.removeIf(new Predicate<ResolvedArtifactResult>() {
                @Override
                boolean test(ResolvedArtifactResult result) {
                    boolean ret = stripAarNames.contains("${result.id.componentIdentifier.displayName}")
                    if (ret) {
                        Log.i 'MergeManifestsHooker', "Stripped manifest of artifact: ${result} -> ${result.file}"
                    }
                    return ret
                }
            })

            hooker.mark()
            return set
        }

        @Override
        Collection<Throwable> getFailures() {
            return origin.getFailures()
        }

        @Override
        Iterator<ResolvedArtifactResult> iterator() {
            return getArtifacts().iterator()
        }

        @Override
        void forEach(Consumer<? super ResolvedArtifactResult> action) {
            getArtifacts().forEach(action)
        }

        @Override
        Spliterator<ResolvedArtifactResult> spliterator() {
            return getArtifacts().spliterator()
        }
    }
}