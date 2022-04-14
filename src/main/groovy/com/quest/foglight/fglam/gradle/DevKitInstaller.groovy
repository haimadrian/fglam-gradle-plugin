package com.quest.foglight.fglam.gradle

import org.gradle.api.Project

/**
 * I created this class to "download and install" dev-kit.<br/>
 * We need dev-kit to be available locally, so AgentCompiler (gluetools library) can find
 * its stuff there.<br/>
 * This class is used by {@link com.quest.foglight.fglam.gradle.task.GeneratorTask} to make sure dev-kit
 * is available for AgentCompiler.<br/>
 * I made this as action to avoid of making Gradle sync way too slow. So this action can be invoked
 * when Gradle executes GeneratorClass only.
 *
 * @author Haim Adrian
 * @since 13-Apr-2022
 */
class DevKitInstaller {
  /**
   * @param project A project to get fglam dev-kit home folder for
   * @return fglam dev-kit home folder
   */
  static String getDevKitHomeDir(Project project) {
    return "${System.getProperty("user.home")}/fog-build/devkit-${project.fglamVersions.getFglamDevkitArchiveVersion()}"
  }

  /**
   * Download fglam archives and untar them to {@link #getDevKitHomeDir dev-kit home folder}<br/>
   * fglam archives are:
   * <ul>
   *  <li>devkit-common-jars.tar.gz</li>
   *  <li>devkit-native.tar.gz (???)</li>
   * </ul>
   * @param project A project to install fglam dev-kit for. (Under its build folder)
   */
  static void installDevKit(Project project) {
    def devkitHome = getDevKitHomeDir(project)
    def devkitHomeDir = new File(devkitHome)
    println "Devkit home dir is: $devkitHomeDir"

    if (!devkitHomeDir.exists()) {
      devkitHomeDir.mkdirs()
      downloadDevkitFromArtifactory(project, devkitHomeDir)
    } else {
      println "Skip downloading devkit archives because $devkitHome already exists"
    }
  }

  private static void downloadDevkitFromArtifactory(Project project, File devkitHomeDir) {
    println 'Downloading and untarring fglam archives, so we can have devkit libs available for AgentCompiler'
    String artifactoryUrl = project.getProperties().get('artifactoryUrl')
    String artifactoryUser = project.getProperties().get('artifactoryUser')
    String artifactoryAPIKey = project.getProperties().get('artifactoryAPIKey')
    String devkitVersion = project.fglamVersions.getFglamDevkitArchiveVersion()

    // In case the properties are not available for project (unit-tests) read them from global Gradle
    // properties, or even try to look in System properties (for Jenkins)
    if (artifactoryUrl == null || artifactoryUrl.isEmpty() || artifactoryUser == null || artifactoryUser.isEmpty()) {
      println 'artifactoryUser is not set as project property. Fallback to gradle global properties'
      def gradlePropsFile = new File("${System.getProperty("user.home")}/.gradle/gradle.properties")
      if (!gradlePropsFile.exists()) {
        println "File not found: ${gradlePropsFile.toString()}"
      } else {
        Properties gradleProperties = new Properties()
        new FileInputStream(gradlePropsFile).with { res ->
          try {
            gradleProperties.load(res)
          } finally {
            res.close()
          }
        }

        artifactoryUrl = getPropertyIfMissing(gradleProperties, 'artifactoryUrl', artifactoryUrl)
        artifactoryUser = getPropertyIfMissing(gradleProperties, 'artifactoryUser', artifactoryUser)
        artifactoryAPIKey = getPropertyIfMissing(gradleProperties, 'artifactoryAPIKey', artifactoryAPIKey)
      }
    }

    def devkitHomeTemp = new File("${devkitHomeDir.getAbsolutePath()}/temp")
    devkitHomeTemp.mkdirs()

    project.ant.get(
        src: "${artifactoryUrl}/libs-release-local/com/quest/foglight/fglam/devkit-common-jars/${devkitVersion}/devkit-common-jars-${devkitVersion}.tar.gz",
        dest: devkitHomeTemp.getPath(),
        username: artifactoryUser,
        password: artifactoryAPIKey
    )

    project.ant.untar(dest: devkitHomeDir.getAbsolutePath(), compression: 'gzip', overwrite: 'false') {
      fileset(dir: devkitHomeTemp.getPath(), includes: "*.tar.gz")
    }

    devkitHomeTemp.deleteDir()
  }

  private static String getPropertyIfMissing(Properties props, String propName, String defaultVal) {
    if (defaultVal == null || defaultVal.isEmpty()) {
      def val = props.get(propName)

      if (val == null || val.isEmpty()) {
        val = System.getProperty(propName)
      }

      return val
    }

    return defaultVal
  }
}
