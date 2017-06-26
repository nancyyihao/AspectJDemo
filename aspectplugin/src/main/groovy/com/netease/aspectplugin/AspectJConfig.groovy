package com.netease.aspectplugin

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.DomainObjectCollection
import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Created by bjwangmingxian on 17/6/23.
 */
public class AspectJConfig {

    private final Project project
    private final BasePlugin plugin
    private final boolean hasAppPlugin
    private final boolean hasLibPlugin


    AspectJConfig(Project project) {
        this.project = project

        hasAppPlugin = project.plugins.hasPlugin(AppPlugin)
        hasLibPlugin = project.plugins.hasPlugin(LibraryPlugin)

        if (!hasAppPlugin && !hasLibPlugin) {
            throw new GradleException("Aspectj: " +
                    "The 'com.android.application' or 'com.android.library' plugin is required.")
        }

        plugin = project.plugins.getPlugin(hasAppPlugin ? AppPlugin : LibraryPlugin)
    }

    /**
     * Return all variants.
     *
     * @return Collection of variants.
     */
    DomainObjectCollection<BaseVariant> getVariants() {
        return hasAppPlugin ? project.android.applicationVariants : project.android.libraryVariants
    }

    List<File> getBootClasspath() {
        if (project.android.hasProperty('bootClasspath')) {
            return project.android.bootClasspath
        } else {
            return plugin.runtimeJarList
        }
    }

}
