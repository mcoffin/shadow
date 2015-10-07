package com.github.jengelman.gradle.plugins.shadow

import com.github.jengelman.gradle.plugins.shadow.tasks.DefaultInheritManifest

import java.io.File

import org.gradle.api.internal.file.FileResolver
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

            manifest = new DefaultInheritManifest(getServices().get(FileResolver.class))

            // By default, inhereit from the project's jar manifest
            manifest.inheritFrom project.jar.manifest

            // Enable the overrides from the NEW osgi manifest
            manifest.inheritFrom(mf) {
                eachEntry {
                    if (OVERRIDE_KEYS.contains(it.getKey())) {
                        it.setValue(it.getMergeValue())
                    } else {
                        it.setValue(it.getBaseValue())
                    }
                }
            }

            classifier = "all-osgi"

            // Copy over all the things from the old jar file, save for the manifest
            // which will be regenerated by the DefaultInheritManifest
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
