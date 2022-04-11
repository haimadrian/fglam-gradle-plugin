package com.quest.foglight.fglam.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

class FglAMVersionsPluginExtension {
    // Compile
    /** Version of log4j2 dependency */
    final Property<String> log4jVersion

    // Test
    /** Version of junit test dependency */
    final Property<String> junitVersion
    /** Version of mockito test dependency */
    final Property<String> mockitoVersion
    /** Version of junit jupyter test dependency */
    final Property<String> junitJupiterVersion

    // Quest dependencies
    /** Version of fglam dev-kit compile dependency */
    final Property<String> fglamDevkitVersion
    /** Version of fglam dev-kit test mock dependency */
    final Property<String> fglamDevkitMockVersion
    /** Version of fglam dev-kit compile dependency */
    final Property<String> fglamDevkitBranch
    /** Version of fglam compile dependency */
    final Property<String> fglamVersion

    FglAMVersionsPluginExtension(ObjectFactory objects) {
        log4jVersion = objects.property(String)
        junitVersion = objects.property(String)
        mockitoVersion = objects.property(String)
        junitJupiterVersion = objects.property(String)
        fglamDevkitVersion = objects.property(String)
        fglamDevkitMockVersion = objects.property(String)
        fglamDevkitBranch = objects.property(String)
        fglamVersion = objects.property(String)

        log4jVersion.set("2.17.1")
        junitVersion.set("4.13.2")
        mockitoVersion.set("4.3.1")
        junitJupiterVersion.set("5.8.2")
        fglamDevkitVersion.set("6.1.0-20211124")
        fglamDevkitMockVersion.set("5.7.1")
        fglamDevkitBranch.set("6.1.0")
        fglamVersion.set("6.1.0")
    }

    /** @see #log4jVersion */
    String getLog4jVersion() {
        return log4jVersion.get()
    }

    /** @see #junitVersion */
    String getJunitVersion() {
        return junitVersion.get()
    }

    /** @see #mockitoVersion */
    String getMockitoVersion() {
        return mockitoVersion.get()
    }

    /** @see #junitJupiterVersion */
    String getJunitJupiterVersion() {
        return junitJupiterVersion.get()
    }

    /** @see #fglamDevkitVersion */
    String getFglamDevkitVersion() {
        return fglamDevkitVersion.get()
    }

    /** @see #fglamDevkitMockVersion */
    String getFglamDevkitMockVersion() {
        return fglamDevkitMockVersion.get()
    }

    /** @see #fglamDevkitBranch */
    String getFglamDevkitBranch() {
        return fglamDevkitBranch.get()
    }

    /** @see #fglamVersion */
    String getFglamVersion() {
        return fglamVersion.get()
    }
}
