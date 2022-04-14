package com.quest.foglight.fglam.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * A Gradle task used for creating a .gar file for Foglight agents.<br/>
 * The task expects agent-manifest to present. See {@link GeneratorTask} and {@link com.quest.glue.tools.compiler.manifest.ManifestGenerator}
 *
 * @author Haim Adrian
 * @since 13-Apr-2022
 */
class GartridgeTask extends DefaultTask {
    private static final String GAR_SUFFIX = ".gar"

    /** Name of the gar file to create. e.g. <code>DockerSwarmAgent</code>. A .gar suffix will be appended to it */
    @Input
    String garName

    /** A reference to the agent-manifest file. (agent.manifest) */
    @InputFile
    File agentManifest

    /** Where to find agent and 3rd party libraries. Defaults to <code>project.buildDir/libs</code> */
    @Optional
    @InputDirectory
    File agentLibDir = new File(project.buildDir, 'libs')

    /** A reference to the output gar file that this task creates */
    @OutputFile
    File outputGarFile = null

    /** Add additional files and directories to the root of the GAR file. [see 'tarfileset' for detailed settings] */
    @Internal
    Closure additionalContents

    /** Add additional files and directories to the root of the GAR file. [see 'tarfileset' for detailed settings] */
    def additionalContents(Closure additionalContents) {
        this.additionalContents = additionalContents
    }

    @TaskAction
    def createGar() {
        if (garName.endsWith(GAR_SUFFIX)) {
            garName = garName.substring(0, garName.length() - GAR_SUFFIX.length())
        }

        // Build gar top level dir layout
        def garDir = new File(project.buildDir, 'gar')
        def garLibDir = new File(new File(garDir, 'contents'), 'lib')
        garLibDir.mkdirs()

        outputGarFile = new File(garDir, garName + GAR_SUFFIX)

        // Copy the contents of the agent's lib dir to the gar lib dir
        def agentLibsFileTree = project.fileTree(agentLibDir)
        agentLibsFileTree.include("**/*.jar", "*.jar")
        agentLibsFileTree.exclude("**/*-source*.jar", "*-source*.jar")
        project.copy {
            from agentLibsFileTree
            into garLibDir
        }

        // Copy the required layout and the rest of the gar contents
        project.copy {
            from agentManifest
            into garLibDir.getParentFile()
        }

        def destFile = outputGarFile.getAbsolutePath()
        println "Creating gar file at: $destFile"
        project.ant.tar(destfile: destFile, compression: "gzip") {
            tarfileset(dir: garLibDir.getParent())

            // Include any user defined filesets
            additionalContents
        }
    }
}
