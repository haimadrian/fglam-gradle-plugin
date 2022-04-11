package com.quest.foglight.fglam.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

import java.text.SimpleDateFormat

class FglAMPluginExtension {
    private static final String BUILD_TIMESTAMP = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date())

    /** Folder under project.buildDir to copy dependencies to */
    final Property<String> dependenciesOutDir

    /** The vendor of cartridges. (Quest Software Inc.) */
    final Property<String> vendor

    FglAMPluginExtension(ObjectFactory objects) {
        dependenciesOutDir = objects.property(String)
        vendor = objects.property(String)

        dependenciesOutDir.set("jars")
        vendor.set("Quest Software Inc.")
    }

    /** Get build date time, in ISO8601, to use in jars manifest */
    static String getBuildISO8601DateTime() {
        return BUILD_TIMESTAMP
    }

    /** @see #dependenciesOutDir */
    String getDependenciesOutDir() {
        return dependenciesOutDir.get();
    }

    /** @see #vendor */
    String getVendor() {
        return vendor.get();
    }
}
