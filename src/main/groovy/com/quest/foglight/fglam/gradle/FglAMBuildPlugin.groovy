package com.quest.foglight.fglam.gradle

import com.quest.foglight.fglam.gradle.plugin.FglAMPluginExtension
import com.quest.foglight.fglam.gradle.plugin.FglAMVersionsPluginExtension
import groovy.io.FileType
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile

/**
 * A gradle plugin created for cartridge team development, to ease build, topology generation, and gar file creation.<br/>
 * For more info and how-to, refer to the README.MD (main page on GitHub repository)
 *
 * @author Haim Adrian
 * @since 13-Apr-2022
 */
class FglAMBuildPlugin implements Plugin<Project> {
  private FglAMVersionsPluginExtension fglamVersionsExtension
  private FglAMPluginExtension fglamExtension

  @Override
  void apply(Project project) {
    applyPlugins(project)
    configureIDE(project)
    configureFglAMVersionsExtension(project)
    configureFglAMExtension(project)
    configureDependencyResolution(project)
    addCommonDependencies(project)

    createCopyDependenciesTask(project)
    setupJarAndSourcesOptions(project)
    createGeneratePermissionsTask(project)
    createGenerateSourcesTaskAndSetupTaskDependencies(project)
    setupJavaCompilerOptions(project)
    setupTestOptions(project)

    setupTasksDependenciesIfCreatingCartridge(project)
    disableUnusedTasks(project)
  }

  private static void applyPlugins(Project project) {
    def applyPluginIfMissing = { String pluginName ->
      if (!project.getPluginManager().hasPlugin(pluginName))
        project.getPluginManager().apply(pluginName)
    }

    applyPluginIfMissing('application')
    applyPluginIfMissing('base')
    applyPluginIfMissing('java')
    applyPluginIfMissing('java-library')
    applyPluginIfMissing('maven-publish')
    applyPluginIfMissing('idea')
    applyPluginIfMissing('eclipse')

    // Had to overcome foglight-gradle-plugin for unit tests.. It screws all tests with its setup
    // and mandatory properties such as artifactoryUrl, User and APIKey.
    if (System.getProperty('test') == null) {
      applyPluginIfMissing('com.quest.foglight')
    }
  }

  private static void configureIDE(Project project) {
    project.idea {
      module {
        downloadSources = true
      }
    }

    project.eclipse {
      classpath {
        downloadSources = true
      }
    }
  }

  private void configureFglAMVersionsExtension(Project project) {
    fglamVersionsExtension = project.extensions.create('fglamVersions', FglAMVersionsPluginExtension)

    // Use project ext so it will be accessible through: project.X, project.ext.X, project.properties.X
    project.ext {
      // Common
      log4jVersion = fglamVersionsExtension.getLog4jVersion()

      // Test
      junitVersion = fglamVersionsExtension.getJunitVersion()
      mockitoVersion = fglamVersionsExtension.getMockitoVersion()
      junitJupiterVersion = fglamVersionsExtension.getJunitJupiterVersion()

      // Quest dependencies
      fglamDevkitVersion = fglamVersionsExtension.getFglamDevkitVersion()
      fglamDevkitMockVersion = fglamVersionsExtension.getFglamDevkitMockVersion()
      fglamDevkitBranch = fglamVersionsExtension.getFglamDevkitBranch()
      fglamVersion = fglamVersionsExtension.getFglamVersion()
    }
  }

  private void configureFglAMExtension(Project project) {
    fglamExtension = project.extensions.create('fglam', FglAMPluginExtension)

    // Add some properties
    project.ext {
      distDir = "${project.buildDir}/dist".toString()
      downloadsDir = "${project.buildDir}/downloaded".toString() // Used, yet not exposed, by foglight-gradle-plugin...
    }
  }

  private void configureDependencyResolution(Project project) {
    project.configurations.all {
      resolutionStrategy {
        // Prefer modules that are part of this build (multi-project or composite build) over external modules
        preferProjectModules()

        // Force certain versions of dependencies (including transitive)
        force "junit:junit:${fglamVersionsExtension.getJunitVersion()}",
            "org.mockito:mockito-core:${fglamVersionsExtension.getMockitoVersion()}",
            "org.apache.logging.log4j:log4j-core:${fglamVersionsExtension.getLog4jVersion()}",
            "org.apache.logging.log4j:log4j-api:${fglamVersionsExtension.getLog4jVersion()}"
      }
    }
  }

  private void addCommonDependencies(Project project) {
    project.dependencies {
      implementation "org.apache.logging.log4j:log4j-core:${fglamVersionsExtension.getLog4jVersion()}"
      implementation "org.apache.logging.log4j:log4j-api:${fglamVersionsExtension.getLog4jVersion()}"

      compileOnly "com.quest.glue:glueapi:${fglamVersionsExtension.getFglamDevkitVersion()}"
      compileOnly "com.quest.glue:gluecore:${fglamVersionsExtension.getFglamDevkitVersion()}"
      compileOnly "com.quest.glue:gluecommon:${fglamVersionsExtension.getFglamDevkitVersion()}"
      compileOnly "com.quest.glue:gluetools:${fglamVersionsExtension.getFglamDevkitVersion()}"
      compileOnly "com.quest.glue:glueapimockimpl:${fglamVersionsExtension.getFglamDevkitMockVersion()}"

      // Used by gluetools, during unit tests...
      testImplementation "com.quest.common:quest-common:${fglamVersionsExtension.getFglamDevkitVersion()}"
      testImplementation "com.quest.common:quest-common-tools:${fglamVersionsExtension.getFglamDevkitVersion()}"
      testImplementation "junit:junit:${fglamVersionsExtension.getJunitVersion()}"
      testImplementation "org.mockito:mockito-core:${fglamVersionsExtension.getMockitoVersion()}"
      testImplementation "org.junit.jupiter:junit-jupiter-api:${fglamVersionsExtension.getJunitJupiterVersion()}"
      testRuntimeOnly "org.junit.vintage:junit-vintage-engine:${fglamVersionsExtension.getJunitJupiterVersion()}"
    }
  }

  /**
   * There is something that brings a corrupted jar file: commons-logging-apache.jar<br/>
   * Delete this file as it should not be in use anyway. The correct library is: commons-logging-VERSION.jar
   * @param directory Directory to look inside, recursively
   */
  private static void deleteGarbageIvyDependencies(File directory) {
    if (directory.exists()) {
      directory.traverse(type: FileType.FILES, nameFilter: ~/commons-logging-apache\.jar/) { file ->
        println "Deleting garbage file: $file"
        file.delete()
      }
    }
  }

  private void createCopyDependenciesTask(Project project) {
    project.tasks.register('copyDependencies', Copy).configure {t ->
      group 'build'
      description "Copy runtimeClasspath to ${fglamExtension.getDependenciesOutDir(project)} directory"
      from project.configurations.runtimeClasspath
      into fglamExtension.getDependenciesOutDir(project)
      include "*.jar"
      exclude "*-source*.jar"
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE
      doFirst {
        deleteGarbageIvyDependencies(new File("${project.ext.downloadsDir}".toString()))
      }
      doLast {
        deleteGarbageIvyDependencies(new File(fglamExtension.getDependenciesOutDir(project)))
      }
    }
  }

  private static void setupJavaCompilerOptions(Project project) {
    project.tasks.withType(JavaCompile).configureEach {
      options.encoding = 'UTF-8'
      // Add this property so we will be able to see deprecation warnings
      options.compilerArgs << "-Xlint:unchecked" << "-Xlint:deprecation"
    }
  }

  private void setupJarAndSourcesOptions(Project project) {
    project.java {
      sourceCompatibility JavaVersion.VERSION_1_8
      targetCompatibility JavaVersion.VERSION_1_8
      withSourcesJar()
    }

    // Must be after evaluate because we access project.configurations.runtimeClasspath, so
    // we need to access after user project is evaluated with its own dependencies.
    // Otherwise we will make problem to the project uses us:
    // Cannot change dependencies of dependency configuration ':apache-agent-trunk:implementation' after it has been included in dependency resolution
    project.afterEvaluate {
      project.tasks.named('sourcesJar').configure { t ->
        classifier = 'sources'
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
          attributes(
              'Implementation-Title': project.name,
              'Implementation-Version': project.version,
              'Implementation-Vendor': fglamExtension.getVendor(),
              'Built-By': System.properties['user.name'],
              'Build-Timestamp': FglAMPluginExtension.getBuildISO8601DateTime(),
              'Created-By': "Gradle ${project.gradle.gradleVersion}",
              'Build-Jdk': "${System.properties['java.version']} (${System.properties['java.vendor']} ${System.properties['java.vm.version']})",
              'Build-OS': "${System.properties['os.name']} ${System.properties['os.arch']} ${System.properties['os.version']}"
          )
        }
        from project.sourceSets.main.allJava
      }

      project.tasks.named('jar').configure {t ->
        dependsOn 'classes'
        destinationDirectory = project.file("${project.buildDir}/libs")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
          attributes(
              'Implementation-Title': project.name,
              'Implementation-Version': project.version,
              'Implementation-Vendor': fglamExtension.getVendor(),
              'Built-By': System.properties['user.name'],
              'Build-Timestamp': FglAMPluginExtension.getBuildISO8601DateTime(),
              'Created-By': "Gradle ${project.gradle.gradleVersion}",
              'Build-Jdk': "${System.properties['java.version']} (${System.properties['java.vendor']} ${System.properties['java.vm.version']})",
              'Build-OS': "${System.properties['os.name']} ${System.properties['os.arch']} ${System.properties['os.version']}",
              'Class-Path': project.configurations.runtimeClasspath.collect { it.getName() }.join(' ')
          )
        }
        doFirst {
          new File(project.ext.distDir).mkdirs()
        }
        doLast {
          // Copy jar for ArchiveArtifacts task
          project.copy {
            from project.tasks.jar
            into fglamExtension.getAdditionalJarOutputDir(project)
          }
        }
      }
    }
  }

  private void setupTestOptions(Project project) {
    project.test {
      useJUnitPlatform()
      testLogging.showStandardStreams = true
      failFast = false
      maxHeapSize = "1024m"

      reports {
        junitXml {
          outputPerTestCase = true
        }
      }
    }
  }

  /**
   * Create the <code>generatePermissions</code> task.<br/>
   * This task responsible for reading all .PERMISSIONS files under resources directory, and generate
   * the FGLAM_PERMISSIONS.MF file with their content.<br/>
   * We also configure classes and compileJava tasks to depend on generatePermissions, thus making sure the permissions
   * manifest is available for jar task.
   * @param project A project to create the task at
   */
  private static void createGeneratePermissionsTask(Project project) {
    project.tasks.register('generatePermissions').configure {t ->
      group 'build'
      description 'Generate FGLAM_PERMISSIONS.MF under META-INF, in case there are .PERMISSIONS files available under resources directory'
      doFirst {
        println 'Running generatePermissions task. If there are .PERMISSIONS files under resources dir, we will have FGLAM_PERMISSIONS.MF as output'
      }
      doLast {
        def permissions = ''
        if (project.sourceSets.main.resources.srcDirs[0].exists()) {
          project.sourceSets.main.resources.srcDirs[0].traverse(type: FileType.FILES, nameFilter: ~/.*.PERMISSIONS/) { file ->
            permissions += file.text
          }
        }

        if (!permissions.isEmpty()) {
          def metaInfDir = new File("${project.sourceSets.main.resources.srcDirs[0].getPath()}/META-INF")
          metaInfDir.mkdirs()
          def permissionsManifest = new File(metaInfDir, 'FGLAM_PERMISSIONS.MF')
          permissionsManifest.text = """Manifest-Version: 1.0
Gradle-Version: Gradle ${project.gradle.gradleVersion}
Created-By: ${System.properties['java.version']} (${System.properties['java.vendor']} ${System.properties['java.vm.version']})
""" + permissions

          println "Generated ${permissionsManifest.toString()} file"
        } else {
          println "Could not find any .PERMISSIONS at: ${project.sourceSets.main.resources.srcDirs[0]}"
        }

        println 'Finished executing "generatePermissions" task'
      }
    }
  }

  private static void createGenerateSourcesTaskAndSetupTaskDependencies(Project project) {
    project.tasks.register('generateSources').configure { t ->
      group 'build'
      description 'This task executes devkit AgentCompiler to generate sources based on agent-definition.xml'
      dependsOn 'copyDependencies'
      finalizedBy 'generatePermissions'
      outputs.dir("${project.buildDir}/tooling/java-gen")
    }

    project.tasks.named('classes').configure { t ->
      dependsOn 'generateSources', 'generatePermissions'
    }

    project.tasks.named('compileJava').configure { t ->
      dependsOn 'generateSources', 'generatePermissions'
    }

    project.tasks.named('sourcesJar').configure {t ->
      dependsOn 'generateSources'
    }

    project.afterEvaluate {
      if (project.tasks.findByName('createGartridge') != null) {
        project.tasks.named('createGartridge').configure {t ->
          dependsOn 'copyDependencies'
        }
      }

      if (project.tasks.findByName('generator') != null) {
        project.tasks.named('generateSources').configure {t ->
          dependsOn 'generator'
        }
      }
    }
  }

  private void setupTasksDependenciesIfCreatingCartridge(Project project) {
    project.afterEvaluate {
      if (project.tasks.findByName('createCartridge') != null) {
        for (taskName in ['build', 'startScripts', 'distTar', 'distZip', 'generateMetadataFileForMavenJavaPublication']) {
          if (project.tasks.findByName(taskName) != null) {
            project.tasks.named(taskName).configure { t ->
              dependsOn 'createCartridge'
            }
          }
        }

        project.tasks.named('publish').configure {t ->
          group 'publishing'
          dependsOn 'createCartridge'
          finalizedBy 'artifactoryPublish'
        }

        project.tasks.named('createCartridge').configure {t ->
          group 'build'
          description 'Creates .car file for this project'
          outputs.file("${project.tasks.createCartridge.getOutputDir()}/${project.tasks.createCartridge.getCarName()}-${project.version}.car")

          doFirst {
            println "Building the ${project.tasks.createCartridge.getCarName()}.car BuildId ${project.foglight.getBuildId()} in ${project.tasks.createCartridge.getOutputDir()}"
          }
          doLast {
            println "Finished creating car file in ${project.tasks.createCartridge.getOutputDir()}. Renaming it to ${project.tasks.createCartridge.getCarName()}-${project.version}.car"
            project.ant.move(todir: "${project.tasks.createCartridge.getOutputDir()}", includeemptydirs: "false") {
              fileset(dir: "${project.tasks.createCartridge.getOutputDir()}", includes: "*.car")
              mapper(type: "glob", from: "*.car", to: "${project.tasks.createCartridge.getCarName()}-${project.version}.car")
            }
          }
        }

        project.publishing {
          publications {
            cartridge(MavenPublication) {
              artifact "${project.tasks.createCartridge.getOutputDir()}/${project.tasks.createCartridge.getCarName()}-${project.version}.car".toString()
            }
          }
        }

        project.artifactory {
          publish {
            defaults {
              publications('cartridge')
              publishBuildInfo = true
              publishPom = true
            }
          }
        }

        if (project.tasks.findByName('createGartridge') != null) {
          project.tasks.named('createCartridge').configure { t ->
            dependsOn 'createGartridge'
          }
        }
      }
    }
  }

  private static void disableUnusedTasks(Project project) {
    project.afterEvaluate {
      for (taskName in ['distTar', 'distZip']) {
        if (project.tasks.findByName(taskName) != null) {
          project.tasks.named(taskName).configure {
            enabled = false
          }
        }
      }
    }
  }
}
