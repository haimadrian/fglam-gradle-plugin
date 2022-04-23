package com.quest.foglight.fglam.gradle.task

import org.gradle.api.DefaultTask
import org.gradle.api.Task
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
 * @see GartridgeTask#garName
 * @see GartridgeTask#agentManifest
 * @see GartridgeTask#agentLibDir
 * @see GartridgeTask#outputGarFile
 * @see GartridgeTask#additionalContents
 */
class GartridgeTask extends DefaultTask {
    private static final String GAR_SUFFIX = ".gar"

    /** Name of the gar file to create. e.g. <code>DockerSwarmAgent</code>. A .gar suffix will be appended to it
     * @see GartridgeTask */
    @Input
    String garName

    /** A reference to the agent-manifest file. (agent.manifest)<br/>
     * <b>Default</b> value is: <code>build/tooling/agent.manifest</code>
     * @see GartridgeTask */
    @Optional
    @InputFile
    File agentManifest = new File(new File(getProject().buildDir, 'tooling'), 'agent.manifest')

    /** Where to find agent and 3rd party libraries.<br/>
     * <b>Default</b> value is: <code>build/libs</code>
     * @see GartridgeTask */
    @Optional
    @InputDirectory
    File agentLibDir = new File(getProject().buildDir, 'libs')

    /** A reference to the output gar file that this task creates<br/>
     * <b>Default</b> value is: <code>build/gar/{garName}.gar</code>
     * @see GartridgeTask */
    @OutputFile
    File outputGarFile = null

    /** Add additional files and directories to the root of the GAR file. [see 'tarfileset' for detailed settings]
     * @see GartridgeTask */
    @Internal
    Closure additionalContents

    /**
     * Constructs a new {@link GartridgeTask}
     */
    GartridgeTask() {
        setGroup('build')
        setDescription('Creates .gar file for this project')
    }

    @Override
    Task configure(Closure closure) {
        // First call super, so we will have garName available
        super.configure(closure)

        dependsOn getProject().tasks.jar

        if (garName.endsWith(GAR_SUFFIX)) {
            garName = garName.substring(0, garName.length() - GAR_SUFFIX.length())
        }

        outputGarFile = new File("${getProject().buildDir}/gar/${garName}${GAR_SUFFIX}".toString()).getAbsoluteFile()

        inputs.file(getProject().tasks.jar.archiveFile)
        outputs.file(outputGarFile)

        println "GartridgeTask input: ${getProject().tasks.jar.archiveFile.get()}"
        println "GartridgeTask output: $outputGarFile"

        // Eventually call super again, to let user to override the defaults
        return super.configure(closure)
    }

    /** Add additional files and directories to the root of the GAR file. [see 'tarfileset' for detailed settings] */
    def additionalContents(Closure additionalContents) {
        this.additionalContents = additionalContents
    }

    @TaskAction
    def createGar() {
        // Build gar top level dir layout
        def garDir = outputGarFile.getParentFile()
        def garContentsDir = new File(garDir, 'contents')
        def garLibDir = new File(garContentsDir, 'lib')
        garLibDir.mkdirs()

        // Copy the contents of the agent's lib dir to the gar lib dir
        def agentLibsFileTree = getProject().fileTree(agentLibDir)
        agentLibsFileTree.include("**/*.jar", "*.jar")
        agentLibsFileTree.exclude("**/*-source*.jar", "*-source*.jar")
        getProject().copy {
            from agentLibsFileTree
            into garLibDir
        }

        // Copy the required layout and the rest of the gar contents
        getProject().copy {
            from agentManifest
            into garContentsDir
        }

        println "Creating gar file at: $outputGarFile"
        getProject().ant.tar(destfile: outputGarFile.getPath(), compression: "gzip") {
            tarfileset(dir: garContentsDir.getPath())

            // Include any user defined filesets
            additionalContents
        }
    }
}
