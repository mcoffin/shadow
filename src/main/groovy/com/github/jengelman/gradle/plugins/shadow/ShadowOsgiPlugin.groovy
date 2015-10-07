package com.github.jengelman.gradle.plugins.shadow

import groovy.io.FileType

import java.io.File

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.osgi.OsgiPluginConvention
import org.gradle.api.plugins.osgi.OsgiManifest

class ShadowOsgiPlugin implements Plugin<Project> {
    static final TASK_UNZIP_SHADOWJAR = "unzipShadowJar"
    static final TASK_COPY_SHADOWJAR_CLASSES = "copyShadowJarClasses"
    static final TASK_OSGI_SHADOWJAR = "osgiShadowJar"

    static final OVERRIDE_KEYS = ["Export-Package", "Import-Package"]

    void createUnzipTask(Project project) {
        project.task(TASK_UNZIP_SHADOWJAR, type: org.gradle.api.tasks.Copy, dependsOn: 'shadowJar') {
            def buildDir = project.getBuildDir().getPath()
            from(project.zipTree(project.shadowJar.archivePath.getPath())) {
                include("**/*.class")
            }
            into("$buildDir/tmp/osgiShadowJar-classes".toString())
        }
    }

    void createOsgiShadowJar(Project project, OsgiManifest mf) {
        project.task(TASK_OSGI_SHADOWJAR, type: org.gradle.api.tasks.bundling.Jar, dependsOn: TASK_UNZIP_SHADOWJAR) {
            def buildDir = project.getBuildDir().getPath()
            manifest = mf
            classifier = "all-osgi"

            from(project.zipTree(project.shadowJar.archivePath.getPath())) {
                exclude("META-INF/MANIFEST.MF")
            }
        }
    }

    @Override
    void apply(Project project) {
        createUnzipTask(project)

        OsgiPluginConvention osgiConvention = project.convention.getPlugin(OsgiPluginConvention.class)
        def mf = osgiConvention.osgiManifest {
            def buildDir = project.getBuildDir().getPath()
            classesDir = new File("$buildDir/tmp/osgiShadowJar-classes".toString())
            classpath = project.configurations.runtime
        }
        createOsgiShadowJar(project, mf)
    }
}
