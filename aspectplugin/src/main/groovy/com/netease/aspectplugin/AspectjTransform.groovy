package com.netease.aspectplugin

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformOutputProvider
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.netease.aspectjplugin.FileUtils
import com.netease.aspectjplugin.JarMerger
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.compile.JavaCompile
import org.aspectj.util.FileUtil
import com.android.SdkConstants



/**
 * Created by bjwangmingxian on 17/6/23.
 */
public class AspectjTransform extends Transform implements TaskExecutionListener {

    static final String ASPECTJ_RUNTIME = "aspectjrt"

    private Project project
    String sourceCompatibility
    String targetCompatibility
    String bootClassPath
    String encoding

    public AspectjTransform(Project project) {
        this.project = project

        def config = new AspectJConfig(project)

        project.afterEvaluate {
            config.variants.all {
                variant ->
                    JavaCompile javaCompile = variant.hasProperty("javaCompiler") ?
                            variant.javaCompiler : variant.javaCompile

                    encoding = javaCompile.options.encoding
                    bootClassPath = config.bootClasspath.join(File.separator)
                    sourceCompatibility = javaCompile.sourceCompatibility
                    targetCompatibility = javaCompile.targetCompatibility
            }
        }
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {

        // clean
        if (!isIncremental) {
            outputProvider.deleteAll()
        }

        if (hasAjcRunTime(inputs)) {
            doAspectTransform(outputProvider, inputs)
        } else {
            doNoThing(outputProvider, inputs)
        }

    }

    private boolean hasAjcRunTime(Collection<TransformInput> inputs) {
        for(TransformInput input : inputs) {
            for (JarInput jarInput : input.jarInputs) {
                if (jarInput.file.absolutePath.contains(ASPECTJ_RUNTIME)) {
                    return true
                }
            }
        }
        return false
    }

    private void doAspectTransform(TransformOutputProvider outputProvider,
                                   Collection<TransformInput> inputs) {

        AspectjWeave aspectjWeave = new AspectjWeave(project)
        aspectjWeave.encoding = encoding
        aspectjWeave.bootClassPath = bootClassPath
        aspectjWeave.sourceCompatibility = sourceCompatibility
        aspectjWeave.targetCompatibility = targetCompatibility

        File resultDir = outputProvider.getContentLocation(
                "aspect", outputTypes, scopes, Format.DIRECTORY)
        if (resultDir.exists()) {
            AspectjLog.i "delete dir ${resultDir.absolutePath}"
            FileUtils.deleteFolder(resultDir)
        }

        FileUtils.mkdirs(resultDir)
        aspectjWeave.destDir = resultDir.absolutePath

        // step 1: read config from project.
        List<String> includeJar = project.extensions.aspectj.includeJarFilter
        List<String> excludeJar = project.extensions.aspectj.excludeJarFilter
        for (TransformInput transformInput : inputs) {
            for (DirectoryInput directoryInput : transformInput.directoryInputs) {
                // put directoryInput.file into Collections.
                aspectjWeave.aspectPath << directoryInput.file
                aspectjWeave.inPath << directoryInput.file
                aspectjWeave.classPath << directoryInput.file
            }

            for (JarInput jarInput : transformInput.jarInputs) {

                aspectjWeave.aspectPath << jarInput.file
                aspectjWeave.classPath << jarInput.file

                String jarPath =jarInput.file.absolutePath
                if (isIncludeFilterMatched(jarPath, includeJar)
                    && !isExcludeFilterMatched(jarPath, excludeJar)) {

                    AspectjLog.i "includeJar---${jarPath}"
                    aspectjWeave.inPath << jarInput.file
                } else {

                    AspectjLog.i "excludeJar---${jarPath}"
                    copyJar(outputProvider, jarInput)
                }
            }
        }

        // step 2: weave code.
        aspectjWeave.weaveCode()

        // step 3: merge jars file.
        AspectjLog.i "aspectj jar merging......"
        handleOutput(resultDir, outputProvider)
        AspectjLog.i "aspect done..................."

    }

    private void doNoThing(TransformOutputProvider outputProvider,
                           Collection<TransformInput> inputs) {
        AspectjLog.i "There is no aspectjrt dependencies in classpath, " +
                "Have you declare in Dependencies ? "
        // just output original files.
        inputs.each {
            TransformInput input ->
                input.directoryInputs.each {
                    DirectoryInput directoryInput ->
                        def dest = outputProvider.getContentLocation(
                                directoryInput.name, directoryInput.contentTypes,
                                directoryInput.scopes, Format.DIRECTORY)
                        // just copy dirs
                        FileUtil.copyDir(directoryInput.file, dest)
                }

                input.jarInputs.each {
                    JarInput jarInput ->
                        def dest = outputProvider.getContentLocation(
                                jarInput.name, jarInput.contentTypes,
                                jarInput.scopes, Format.JAR)
                        // just copy jar files
                        FileUtil.copyDir(jarInput.file, dest)
                }
        }
    }

    private void handleOutput(File resultDir, TransformOutputProvider outputProvider) {
        //add class file to aspect result jar
        if (resultDir.listFiles().length > 0) {
            File jarFile = outputProvider.getContentLocation(
                    "aspected", outputTypes, scopes, Format.JAR)
            FileUtils.mkdirs(jarFile.parentFile)
            FileUtils.deleteIfExists(jarFile)

            JarMerger jarMerger = new JarMerger(jarFile)
            try {
                jarMerger.setFilter(new JarMerger.IZipEntryFilter() {
                    @Override
                    public boolean checkEntry(String archivePath)
                            throws JarMerger.IZipEntryFilter.ZipAbortException {
                        return archivePath.endsWith(SdkConstants.DOT_CLASS)
                    }
                });


                jarMerger.addFolder(resultDir)
            } catch (Exception e) {
                throw new TransformException(e)
            } finally {
                jarMerger.close()
            }

        }

        FileUtils.deleteFolder(resultDir)
    }

    @Override
    void beforeExecute(Task task) {
        AspectjLog.i (task.name, "AspectJTransform beforeExecute ....")
    }

    @Override
    void afterExecute(Task task, TaskState taskState) {
        AspectjLog.i (task.name, "AspectJTransform afterExecute ....")
    }

    @Override
    String getName() {
        return "AspectjTransform"
    }


    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return Sets.immutableEnumSet(QualifiedContent.DefaultContentType.CLASSES)
    }

    /**
     *配置当前Transform的影响范围,相当于作用域
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return ImmutableSet.<QualifiedContent.Scope>of(
                QualifiedContent.Scope.PROJECT,
                // local dependencies e.g jar, aar
                QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
                // sub project
                QualifiedContent.Scope.SUB_PROJECTS,
                // sub project dependencies
                QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
                // external libs
                QualifiedContent.Scope.EXTERNAL_LIBRARIES);
    }

    @Override
    boolean isIncremental() {
        return false
    }


    boolean isExcludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.EXCLUDE)
    }

    boolean  isIncludeFilterMatched(String str, List<String> filters) {
        return isFilterMatched(str, filters, FilterPolicy.INCLUDE)
    }

    boolean isFilterMatched(String str, List<String> filters, FilterPolicy filterPolicy) {
        if(str == null) {
            return false
        }

        if (filters == null || filters.isEmpty()) {
            return filterPolicy == FilterPolicy.INCLUDE
        }

        for (String s : filters) {
            if (isContained(str, s)) {
                return true
            }
        }

        return false
    }

    boolean copyJar(TransformOutputProvider outputProvider, JarInput jarInput) {
        if (outputProvider == null || jarInput == null) {
            return false
        }

        String jarName = jarInput.name
        if (jarName.endsWith(".jar")) {
            jarName = jarName.substring(0, jarName.length() - 4)
        }

        File dest = outputProvider.getContentLocation(jarName, jarInput.contentTypes, jarInput.scopes, Format.JAR)

        FileUtil.copyFile(jarInput.file, dest)

        return true
    }

    static boolean isContained(String str, String filter) {
        if (str == null) {
            return false
        }

        String filterTmp = filter
        if (str.contains(filterTmp)) {
            return true
        } else {
            if (filterTmp.contains("/")) {
                return str.contains(filterTmp.replace("/", File.separator))
            } else if (filterTmp.contains("\\")) {
                return str.contains(filterTmp.replace("\\", File.separator))
            }
        }

        return false
    }

    enum FilterPolicy {
        INCLUDE
        , EXCLUDE
    }
}
