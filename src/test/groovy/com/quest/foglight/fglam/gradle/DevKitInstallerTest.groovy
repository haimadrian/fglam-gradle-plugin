package com.quest.foglight.fglam.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import static org.junit.jupiter.api.Assertions.*

/**
 * Test {@link DevKitInstaller} class
 *
 * @author Haim Adrian
 * @since 13-Apr-2022
 */
class DevKitInstallerTest {
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
      artifactoryUrl = 'https://artifactory.labs.quest.com/artifactory'
      artifactoryUser = '' // So installer will read gradle.properties from home folder
      artifactoryAPIKey = ''
    }

    project.getPluginManager().apply("com.quest.foglight.fglam")
  }

  @Test
  void testInstallingDevkit() {
    DevKitInstaller.installDevKit(project)

    def devkitHome = new File(DevKitInstaller.getDevKitHomeDir(project))

    assertTrue(devkitHome.exists(), "Devkit was not installed. Not found: $devkitHome")
  }
}
