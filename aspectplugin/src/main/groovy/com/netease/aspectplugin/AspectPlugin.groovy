package com.netease.aspectplugin

import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
public class AspectPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

        println "hello, this is aspectj plugin!"


        AspectjLog.i "AspectPlugin apply run......"

        AspectjLog.i "register AspectJTransform......"

        def hasAppPlugin = project.plugins.hasPlugin(AppPlugin)
        if (hasAppPlugin) {
            AspectjTransform aspectjTransform = new AspectjTransform(project)
            project.gradle.addListener(aspectjTransform)
            project.android.registerTransform(aspectjTransform)
        }
    }
}