package com.dovar.dplugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.TaskManager
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.core.VariantConfiguration
import com.android.build.gradle.internal.tasks.factory.TaskFactory
import com.android.build.gradle.internal.variant.VariantFactory
import com.android.builder.core.VariantTypeImpl
import com.dovar.dplugin.tasks.AssemblePlugin
import com.dovar.dplugin.utils.Reflect
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.reflect.Instantiator
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.gradle.util.NameMatcher

import javax.inject.Inject
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Base class of plugin, we create assemblePlugin task here
 */
public abstract class BasePlugin implements Plugin<Project> {

    protected Project project
    protected Instantiator instantiator
    protected TaskFactory taskFactory

    boolean checkVariantFactoryInvoked

    @Inject
    public BasePlugin(Instantiator instantiator, ToolingModelBuilderRegistry registry) {
        this.instantiator = instantiator
    }

    @Override
    public void apply(Project project) {
        this.project = project
        AppPlugin appPlugin = project.plugins.findPlugin(AppPlugin)

        Reflect reflect = Reflect.on(appPlugin.variantManager)

        VariantFactory variantFactory = Proxy.newProxyInstance(this.class.classLoader, [VariantFactory.class] as Class[],
                new InvocationHandler() {
                    Object delegate = reflect.get('variantFactory')

                    @Override
                    Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ('preVariantWork' == method.name) {
                            checkVariantFactoryInvoked = true
                            boolean isBuildingPlugin = evaluateBuildingPlugin(appPlugin, project)
                            beforeCreateAndroidTasks(isBuildingPlugin)
                        }

                        return method.invoke(delegate, args)
                    }
                })
        reflect.set('variantFactory', variantFactory)

        project.extensions.create('dpluginConfig', DExtention)
        TaskManager taskManager = Reflect.on(appPlugin).field('taskManager').get()
        taskFactory = taskManager.getTaskFactory()
        project.afterEvaluate {

            if (!checkVariantFactoryInvoked) {
                throw new RuntimeException('Evaluating DPlugin\'s configurations has failed!')
            }

            android.applicationVariants.each { ApplicationVariantImpl variant ->
                if ('release' == variant.buildType.name) {
                    String variantAssembleTaskName = variant.variantData.scope.getTaskName('assemble', 'Plugin')
                    def final variantPluginTaskName = createPluginTaskName(variantAssembleTaskName)
                    final def configAction = new AssemblePlugin.ConfigAction(project, variant)

                    taskFactory.register(variantPluginTaskName, AssemblePlugin, configAction)

                    Action action = new Action<Task>() {
                        @Override
                        void execute(Task task) {
                            task.dependsOn(variantPluginTaskName)
                        }
                    }
                    taskFactory.configure("assemblePlugin", action)
                }
            }
        }

        project.task('assemblePlugin', dependsOn: "assembleRelease", group: 'build', description: 'Build plugin apk')
    }

    String createPluginTaskName(String name) {
        if (name == 'assembleReleasePlugin') {
            return '_assemblePlugin'
        }
        return name.replace('Release', '')
    }

    private boolean evaluateBuildingPlugin(AppPlugin appPlugin, Project project) {
        def startParameter = project.gradle.startParameter
        def targetTasks = startParameter.taskNames

        def pluginTasks = ['assemblePlugin'] as List<String>

        appPlugin.variantManager.buildTypes.each {
            def buildType = it.value.buildType
            if ('release' != buildType.name) {
                return
            }
            if (appPlugin.variantManager.productFlavors.isEmpty()) {
                return
            }

            appPlugin.variantManager.productFlavors.each {
                String variantName
                //是 VariantTypeImpl.BASE_APK 还是 VariantTypeImpl.OPTIONAL_APK？
                variantName = VariantConfiguration.computeRegularVariantName(it.key, buildType, VariantTypeImpl.BASE_APK, null)
                def variantPluginTaskName = createPluginTaskName("assemble${variantName.capitalize()}Plugin".toString())
                pluginTasks.add(variantPluginTaskName)
            }
        }

//        pluginTasks.each {
//            Log.i 'VAPlugin', "pluginTask: ${it}"
//        }

        boolean isBuildingPlugin = false
        NameMatcher nameMatcher = new NameMatcher()
        targetTasks.every {
            int index = it.lastIndexOf(":");
            String task = index >= 0 ? it.substring(index + 1) : it
            String taskName = nameMatcher.find(task, pluginTasks)
            if (taskName != null) {
//                Log.i 'VAPlugin', "Found task name '${taskName}' by given name '${it}'"
                isBuildingPlugin = true
                return false
            }
            return true
        }

        return isBuildingPlugin
    }

    protected abstract void beforeCreateAndroidTasks(boolean isBuildingPlugin)

    protected final DExtention getDPluginConfig() {
        return this.project.dpluginConfig
    }

    protected final AppExtension getAndroid() {
        return this.project.android
    }
}
