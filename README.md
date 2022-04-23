# fglam-gradle-plugin
A Foglight Gradle plugin for cartridges team, developed by Haim Adrian

Developed using Gradle 7.4.2

Plugin is published to https://artifactory.labs.quest.com/ui/repos/tree/General/libs-release-local/com/quest/foglight/fglam

Make sure you also apply foglight-gradle-plugin

### Usage
#### Add our maven repo, and declare dependency
    buildscript {
        repositories {
            maven {
                url "$artifactoryUrl/libs-release"
                credentials { 
                    username artifactoryUser
                    password artifactoryAPIKey
                }
            }
        }

        dependencies {
            classpath "com.quest.foglight:foglight-gradle-plugin:1.17"
            classpath "com.quest.foglight.fglam:fglam-gradle-plugin:1.0"
        }
    }

    apply plugin: 'com.quest.foglight'
    apply plugin: 'com.quest.foglight.fglam'

#### Customize extensions
Those are the defaults

    fglam {
        dependenciesOutDir = 'libs'
        additionalJarOutputDir = 'dist'
        vendor = 'Quest Software Inc.'
    }

    fglamVersions {
        log4jVersion = '2.17.1'
        junitVersion = '4.13.2'
        mockitoVersion = '4.3.1'
        junitJupiterVersion = '5.8.2'
        fglamDevkitVersion = '6.1.0-20211124'
        fglamDevkitArchiveVersion = '6.1.0' // Devkit installation is for GeneratorTask
        fglamDevkitMockVersion = '5.7.1'
        fglamDevkitBranch = '6.1.0'
        fglamVersion = '6.1.0'
    }

#### Use GeneratorTask to generate topology classes
This task relies on dev-kit installation. It will download and install it automatically. See `DevKitInstaller` class  
Note that we also use foglight extension, so make sure you set-up `foglight.branch` property.

    foglight {
        branch = '6.3.0'
    }

    tasks.register('generator', com.quest.foglight.fglam.gradle.task.GeneratorTask).configure { t-> 
        dependsOn tasks.copyDependencies  // By Default.
        outputs.dir(toolingDirectory) // By Default. - generated sources

        // The file will be copied and updated, to toolingDirectory. We also copy other files next to agent-definition.xml
        agentDefinition = file("$configDir/agent-definition.xml") 
        runAllGenerators = true  // By Default.
        toolingDirectory = file("${project.buildDir}/tooling")  // By Default.
        
        // Additional properties (Not sure AgentCompiler really relies on them, yet they are there for backward cmpatibiliy: GeneratorTask.*
    }

If you use default values only, it is that simple:

    tasks.register('generator', com.quest.foglight.fglam.gradle.task.GeneratorTask).configure { t ->
        agentDefinition = file("$configDir/agent-definition.xml") 
    }

#### Use GartridgeTask to create .gar file
    tasks.register('createGartridge', com.quest.foglight.fglam.gradle.task.GartridgeTask).configure { t ->
        dependsOn tasks.jar  // By Default.
        inputs.file(tasks.jar.archiveFile)  // By Default.
        outputs.file(outputGarFile)  // By Default.

        garName = 'DockerSwarmAgent'
        agentManifest = file("${project.buildDir}/tooling/agent.manifest")  // By Default.
        agentLibDir = file("${project.buildDir}/libs")  // By Default.
        outputGarFile = file("${project.buildDir}/gar/${garName}.gar")  // By Default.

        additionalContents() {
            // If you have more filesets to add to the gar file
        }
    }

If you use default values only, it is that simple:

    tasks.register('createGartridge', com.quest.foglight.fglam.gradle.task.GartridgeTask).configure { t ->
        garName = 'DockerSwarmAgent'
    }

#### The `generateSources` task
The plugin will set-up task dependencies automatically.  
This way you are able to make `generator` task to be executed automatically. If `generator` task presents, it will be executed automatically as part of `generateSources`.  
Also, the `fglam-gradle-plugin` automatically configures `compileJava` to depend on `generateSources`, and finalizes `generateSources` by `generatePermissions`. Thus saving you task definitions


### Some auto-settings
* Automatically apply the plugins below in case they were not applied already:
  * application
  * base
  * java
  * java-library
  * maven-publish
  * idea
  * eclipse
  * foglight-gradle-plugin
* Java `sourceCompatibility` and `targetCompatibility` are set to `JavaVersion.VERSION_1_8`
* Project is configured to create sources jar as well
* Both jar and sources jar will have meaningful information in their manifest files. Jar's manifest will also include Class-Path.
* There will be a `copyDependencies` task which copies runtimeClasspath of the project to `project.buildDir/fglam.dependenciesOutDir` (default: build\libs)
* Common dependencies will be declared for the project:
  * `implementation`: log4j-api and log4j-core
  * `compileOnly`: glueapi, gluecore, gluecommon, gluetools, glueapimockimpl
  * `testImplementation`: quest-common, quest-common-tools, junit, mockito-core
* There will be a `generatePermissions` task which looks for .PERMISSIONS files under resources folder. In case there are, it will generate FGLAM_PERMISSIONS.MF file under META-INF, and it will be done automatically, cause we configure `compileJava` task to depend on `generatePermissions`.
* In case `createCartridge` task presents, we will add information messages to it, and make sure the file name contains project version. e.g. CAR_NAME-6.3.0-BUILD_ID.car, instead of CAR_NAME.car. We will also copy it to fglam.additionalJarOutputDir, to ease uploading it to Jenkins. This way you do not have to duplicate the messages.

        project.tasks.named('createCartridge').configure { t ->
          group 'build'
          description 'Creates .car file for this project'

          doFirst {
            println "Building the ${project.tasks.createCartridge.getCarName()}.car BuildId ${project.foglight.getBuildId()} in ${project.tasks.createCartridge.getOutputDir()}"
          }
          doLast {
            println "Finished creating car file in ${project.tasks.createCartridge.getOutputDir()}. Renaming it to ${project.tasks.createCartridge.getCarName()}-${project.version}.car"
            project.ant.move(todir: "${project.tasks.createCartridge.getOutputDir()}", includeemptydirs: "false") {
              fileset(dir: "${project.tasks.createCartridge.getOutputDir()}", includes: "*.car")
              mapper(type: "glob", from: "*.car", to: "${project.tasks.createCartridge.getCarName()}-${project.version}.car")
            }

            println "Copying cartridge to ${fglamExtension.getAdditionalJarOutputDir(project)}/${project.tasks.createCartridge.getCarName()}-${project.version}.car"
            project.copy {
              from project.tasks.createCartridge.getOutputDir()
              into fglamExtension.getAdditionalJarOutputDir(project)
              include '*.car'
            }
          }
        }
* In case `createCartridge` task presents, we will create `cartridge` MavenPublication to publish the outputs of `createCartridge` task. Thus you do not have to configure `publishing` and `artifactory` plugins.
* This plugin adds some useful properties to a configured project, so you can re-use them. (through project.\_, project.ext.\_, or project.properties.\_)
  * `distDir` = "${project.buildDir}/dist"
  * `downloadsDir` = "${project.buildDir}/downloaded"  // This is where foglight-gradle-plugin downloads ivy dependencies to
* Disabled the tasks: `distTar` and `distZip` as they are irrelevant. We use Gartridge and Cartridge tasks
* Configured `eclipse` and `idea` plugins to download sources automatically, so it will be nicer to wok and see documentation and sources
