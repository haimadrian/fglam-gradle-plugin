package com.quest.foglight.fglam.gradle

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.internal.project.DefaultProject
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import java.util.stream.Collectors

import static org.junit.jupiter.api.Assertions.*

/**
 * Test {@link FglAMBuildPlugin} class
 *
 * @author Haim Adrian
 * @since 13-Apr-2022
 */
class FglAMBuildPluginTest {
  private Project project

  @BeforeAll
  static void setupClass() {
    // See FglAMBuildPlugin.applyPlugins for more details
    System.setProperty('test', 'to ignore foglight-gradle-plugin')
  }

  @BeforeEach
  void setup() {
    project = ProjectBuilder.builder().withName('test-project').build()
    project.repositories {
      mavenLocal()
      mavenCentral()
    }

    // Mandatory properties used by foglight-gradle-plugin.
    project.ext {
      artifactoryUrl = 'F'
      artifactoryUser = 'M'
      artifactoryAPIKey = 'L'
    }

    project.getPluginManager().apply("com.quest.foglight.fglam")

    // Call evaluate to make sure "project.afterEvaluate" is invoked. (For jar and sourcesJar tasks)
    ((DefaultProject)project).evaluate()
  }

  @Test
  void testProjectHasPlugin() {
    assertTrue(project.getPluginManager().hasPlugin("com.quest.foglight.fglam"))
  }

  @Test
  void testProjectHasDependencies() {
    def dependenciesName = () -> project.configurations.runtimeClasspath.resolve().stream().map(f -> f.toString().substring(f.toString().lastIndexOf(File.separator) + 1))
    println "runtimeClasspath: ${dependenciesName.call().collect(Collectors.toSet())}"

    def log4jCore = "log4j-core-${project.fglamVersions.getLog4jVersion()}.jar"
    def log4jApi = "log4j-api-${project.fglamVersions.getLog4jVersion()}.jar"
    assertTrue(dependenciesName.call().anyMatch(x -> x.equalsIgnoreCase(log4jCore)), "$log4jCore does not exist in runtimeClasspath")
    assertTrue(dependenciesName.call().anyMatch(x -> x.equalsIgnoreCase(log4jApi)), "$log4jApi does not exist in runtimeClasspath")
  }

  @Test
  void testProjectHasTasks() {
    assertTrue(project.tasks.findByName('jar') != null, "jar task is missing")
    assertTrue(project.tasks.findByName('sourcesJar') != null, "sourcesJar task is missing")
  }

  @Test
  void testProjectHasJava8() {
    assertEquals(project.java.sourceCompatibility, JavaVersion.VERSION_1_8, "sourceCompatibility is wrong")
    assertEquals(project.java.targetCompatibility, JavaVersion.VERSION_1_8, "targetCompatibility is wrong")
  }

  @Test
  void testJarTask_verifyManifest() {
    Map<String, String> manifestAttributes = project.tasks.named('jar').get().manifest.attributes
    println manifestAttributes

    manifestAttributesAssertions(manifestAttributes)
    assertTrue(manifestAttributes.containsKey('Class-Path'), 'Manifest entry is missing')
  }

  @Test
  void testSourcesJarTask_verifyManifest() {
    Map<String, String> manifestAttributes = project.tasks.named('sourcesJar').get().manifest.attributes
    println manifestAttributes

    manifestAttributesAssertions(manifestAttributes)
    assertFalse(manifestAttributes.containsKey('Class-Path'), 'Manifest entry should not exist in sources')
  }

  private void manifestAttributesAssertions(Map<String, String> manifestAttributes) {
    assertTrue(manifestAttributes.containsKey('Implementation-Title'), 'Manifest entry is missing')
    assertTrue(manifestAttributes.containsKey('Implementation-Version'), 'Manifest entry is missing')
    assertTrue(manifestAttributes.containsKey('Implementation-Vendor'), 'Manifest entry is missing')
    assertTrue(manifestAttributes.containsKey('Built-By'), 'Manifest entry is missing')
    assertTrue(manifestAttributes.containsKey('Build-Timestamp'), 'Manifest entry is missing')
    assertTrue(manifestAttributes.containsKey('Build-Jdk'), 'Manifest entry is missing')

    assertTrue(project.name == manifestAttributes['Implementation-Title'], 'Implementation-Title supposed to be project name')
    assertTrue('Quest Software Inc.' == manifestAttributes['Implementation-Vendor'], 'Implementation-Vendor supposed to be Quest Software Inc.')
  }
}
