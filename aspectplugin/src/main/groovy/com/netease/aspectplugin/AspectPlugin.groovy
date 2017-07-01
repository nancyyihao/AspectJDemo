package com.netease.aspectplugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
public class AspectPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        AspectjLog.i "register AspectJTransform......"

        /**
         * Create an aspectj extension so you can use in your build.gradle file.
         * e.g.
         * <p>
         * aspectj {
         *     includeJarFilter com.XXX.XXX, org.XXX.XXX
         *     excludeJarFilter
         * }
         * </p>
         */
        project.extensions.create("aspectj", AspectjExtension)

        def hasAppPlugin = project.plugins.hasPlugin(AppPlugin)
        def hasLibPlugin = project.plugins.hasPlugin(LibraryPlugin)

        if (hasAppPlugin || hasLibPlugin) {
            AspectjTransform aspectjTransform = new AspectjTransform(project)
            project.gradle.addListener(aspectjTransform)
            project.android.registerTransform(aspectjTransform)
        } else {
            throw new GradleException("Aspectj: " +
                    "The 'com.android.application' or 'com.android.library' " +
                    "plugin is required.")
        }
    }
}