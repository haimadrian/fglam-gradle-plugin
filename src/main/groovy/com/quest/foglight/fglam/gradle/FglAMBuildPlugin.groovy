package com.quest.foglight.fglam.gradle


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile

class FglAMBuildPlugin implements Plugin<Project> {
  private FglAMVersionsPluginExtension fglamVersionsExtension
  private FglAMPluginExtension fglamExtension

  @Override
  void apply(Project project) {
    configureFglAMVersionsExtension(project)
    configureFglAMExtension(project)
    configureAnt(project)
    configureDependencyResolution(project)
    configureGlueCoreDependency(project)

    createCopyDependenciesTask(project)
    setupJavaCompilerOptions(project)
    setupJarAndSourcesOptions(project)
  }

  private void configureFglAMVersionsExtension(Project project) {
    fglamVersionsExtension = project.extensions.create('fglamVersions', FglAMVersionsPluginExtension)

    project.properties.computeIfAbsent('log4jVersion', k -> fglamVersionsExtension.getLog4jVersion())

    // Test
    project.properties.computeIfAbsent('junitVersion', k -> fglamVersionsExtension.getJunitVersion())
    project.properties.computeIfAbsent('mockitoVersion', k -> fglamVersionsExtension.getMockitoVersion())
    project.properties.computeIfAbsent('junitJupiterVersion', k -> fglamVersionsExtension.getJunitJupiterVersion())

    // Quest dependencies
    project.properties.computeIfAbsent('fglamDevkitVersion', k -> fglamVersionsExtension.getFglamDevkitVersion())
    project.properties.computeIfAbsent('fglamDevkitMockVersion', k -> fglamVersionsExtension.getFglamDevkitMockVersion())
    project.properties.computeIfAbsent('fglamDevkitBranch', k -> fglamVersionsExtension.getFglamDevkitBranch())
    project.properties.computeIfAbsent('fglamVersion', k -> fglamVersionsExtension.getFglamVersion())
  }

  private void configureFglAMExtension(Project project) {
    fglamExtension = project.extensions.create('fglam', FglAMPluginExtension)
  }

  private static void configureAnt(Project project) {
    project.beforeEvaluate {
      // Ensure that ant INFO log messages are displayed
      project.ant.lifecycleLogLevel = "INFO"

      // Set some ant properties used to resolve references, etc.
      project.ant.properties['artifactoryUrl'] = project.property("artifactoryUrl")
      project.ant.properties['artifactoryUser'] = project.property("artifactoryUser")
      project.ant.properties['artifactoryAPIKey'] = project.property("artifactoryAPIKey")
    }
  }

  private void configureDependencyResolution(Project project) {
    project.configurations.all {
      resolutionStrategy {
        // Prefer modules that are part of this build (multi-project or composite build) over external modules
        preferProjectModules()

        // Force certain versions of dependencies (including transitive)
        force "junit:junit:${fglamVersionsExtension.getJunitVersion()}",
                "org.mockito:mockito-core:${fglamVersionsExtension.getMockitoVersion()}"
      }
    }
  }

  private void configureGlueCoreDependency(Project project) {
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

  private void createCopyDependenciesTask(Project project) {
    project.tasks.register('copyDependencies', Copy).configure {
      group 'build'
      from project.configurations.runtimeClasspath
      into "${project.buildDir}/${fglamExtension.getDependenciesOutDir()}"
      include "*.jar"
      exclude "*-source*.jar"
      duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    project.tasks.named('classes').configure {
      dependsOn project.tasks.copyDependencies
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
    project.afterEvaluate {
      project.tasks.named('sourcesJar').configure {
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

      project.tasks.named('jar').configure {
        dependsOn 'classes'
        destinationDirectory = project.file("${project.projectDir}/dist")
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
      }
    }
  }
}
