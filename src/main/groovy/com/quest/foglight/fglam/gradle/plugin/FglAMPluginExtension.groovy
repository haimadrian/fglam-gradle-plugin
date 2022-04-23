package com.quest.foglight.fglam.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import java.text.SimpleDateFormat

/**
 * Extension to let users to specify custom dependencies output directory, or modify vendor.
 *
 * @author Haim Adrian
 * @since 13-Apr-2022
 */
class FglAMPluginExtension {
    private static final String BUILD_TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date())

    /** Folder under project.buildDir to copy dependencies to. Default value is 'libs' */
    final Property<String> dependenciesOutDir

    /** The vendor of cartridges. (Quest Software Inc.) */
    final Property<String> vendor

    /** Folder under project.buildDir to copy output jar to. Default value is 'dist'.
     * We do it so we can have an empty folder for archiving artifacts to Jenkins */
    final Property<String> additionalJarOutputDir

    FglAMPluginExtension(ObjectFactory objects) {
        dependenciesOutDir = objects.property(String)
        vendor = objects.property(String)
        additionalJarOutputDir = objects.property(String)

        dependenciesOutDir.set("libs")
        additionalJarOutputDir.set("dist")
        vendor.set("Quest Software Inc.")
    }

    /** Get build date time, in ISO8601, to use in jars manifest */
    static String getBuildISO8601DateTime() {
        return BUILD_TIMESTAMP
    }

    /** @see #dependenciesOutDir */
    String getDependenciesOutDir(Project project) {
        return "${project.buildDir}/${dependenciesOutDir.get()}".toString()
    }

    /** @see #additionalJarOutputDir */
    String getAdditionalJarOutputDir(Project project) {
        return "${project.buildDir}/${additionalJarOutputDir.get()}".toString()
    }

    /** @see #vendor */
    String getVendor() {
        return vendor.get()
    }
}
