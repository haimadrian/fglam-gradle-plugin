package com.quest.foglight.fglam.gradle.task

import com.quest.foglight.fglam.gradle.DevKitInstaller
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat

/**
 * A Gradle task used for generating topology classes based on agent-definition and options.<br/>
 * The task relies on gluetools module. See {@link com.quest.glue.tools.AgentCompiler}.<br/>
 *
 * @author Haim Adrian
 * @since 13-Apr-2022
 * @see com.quest.glue.tools.AgentCompiler
 */
class GeneratorTask extends DefaultTask {
    private static final String FGLAM_TOOLING_CART_BUILD_ID_PROPERTY = "fglam.tooling.cart.buildId"
    private static final String BUILD_TIMESTAMP = new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date())

    /** A reference to agent-definition.xml file to be used for generating topology classes.
     * If the file is not under {@link #toolingDirectory}, we will copy it with its siblings, and
     * update agent definition according to project version. (Replacing product.version token) */
    @InputFile
    File agentDefinition

    @Input
    Boolean runAllGenerators = true

    /** @see com.quest.glue.tools.compiler.cdt.CDTGenerator */
    @Optional
    @Input
    Boolean generateCDT = false

    /** @see com.quest.glue.tools.compiler.topology.TopologyXMLGenerator */
    @Optional
    @Input
    Boolean generateTopology = false

    /** @see com.quest.glue.tools.compiler.topology.TopologyUMLGenerator */
    @Optional
    @Input
    Boolean generateUML = false

    /** Whether to include deprecated topology properties or not */
    @Optional
    @Input
    Boolean includeDeprecatedTopologyProperties = true

    /** For backward compatability. Default value is version-timestamp. e.g 6.3.0-20220413-1155 */
    @Optional
    @Input
    String buildNumber = null

    /** For backward compatability.
     * @see com.quest.glue.tools.compiler.property.fmsconfig.MonitoringPolicyGenerator */
    @Optional
    @Input
    Boolean generateMonitoringPolicy = false

    /** For backward compatability.
     * @see com.quest.glue.tools.compiler.property.asps.ASPServiceGenerator */
    @Optional
    @Input
    Boolean generateAspWrappers = false

    /** For backward compatability.
     * @see com.quest.glue.tools.compiler.manifest.ManifestGenerator */
    @Optional
    @Input
    Boolean generateAgentManifest = false

    /** For backward compatability.
     * @see com.quest.glue.tools.compiler.topology.TopologyTemplateGenerator */
    @Optional
    @Input
    Boolean generateTopologySubmitter = false

    /** For backward compatability.
     * @see com.quest.glue.tools.compiler.CallbackCodeGenerator */
    @Optional
    @Input
    Boolean generateCallbackCode = false

    /** For backward compatability.
     * @see com.quest.glue.tools.compiler.datacollector.CollectorInterfaceGenerator */
    @Optional
    @Input
    Boolean generateCollectorInterface = false

    /** Output directory, where generated files will be found. It is project.buildDir/tooling */
    @OutputDirectory
    File toolingDirectory

    @TaskAction
    def generator() {
        if ((agentDefinition == null) || !agentDefinition.exists()) {
            println 'You must specify agentDefinition.. Like.. what am I supposed to do?'
            throw new FileNotFoundException('NOT_FOUND: agent-definition.xml. Please specify agentDefinition property and make sure file exists.')
        }

        if (toolingDirectory == null) {
            toolingDirectory = new File(project.buildDir, 'tooling')
        }

        toolingDirectory.mkdirs()

        def javaGenDir = new File(toolingDirectory, 'java-gen')

        // In case the folder is not empty it will cause AgentCompiler to fail. So just skip
        if (javaGenDir.exists()) {
            println "$javaGenDir already exists. Nothing will be generated. To force generate, run 'clean' first."
            return
        }

        optionallyUpdateAgentDefinition()

        DevKitInstaller.installDevKit(project)

        if (buildNumber == null) {
            buildNumber = "${project.foglight.version}-$BUILD_TIMESTAMP"
        }

        System.setProperty(FGLAM_TOOLING_CART_BUILD_ID_PROPERTY, buildNumber)
        System.setProperty('quest.home', DevKitInstaller.getDevKitHomeDir(project))

        def cmdArgs = buildCmdArgs()

        println "Running AgentCompiler with arguments: ${Arrays.toString(cmdArgs)}"
        def responseCode = com.quest.glue.tools.AgentCompiler.internalMain(cmdArgs)
        println "Response code of AgentCompiler is: $responseCode"
    }

    private void optionallyUpdateAgentDefinition() {
        if (!agentDefinition.getParent().equalsIgnoreCase(toolingDirectory.getPath())) {
            println "Updating agent-definition.xml and copy it with its sibling files to $toolingDirectory"

            project.ant.copy(file: agentDefinition.getPath(), todir: toolingDirectory.getPath(), overwrite: "true", outputencoding: StandardCharsets.UTF_8.name()) {
                filterset {
                    filter(token: "product.version", value: project.foglight.branch.get())
                }
            }

            def siblings = agentDefinition.getParentFile().listFiles(new FilenameFilter() {
                boolean accept(File dir, String name) {
                    return name != agentDefinition.getName()
                }
            })

            siblings.each { sibling ->
                project.ant.copy(file: sibling.getPath(), todir: toolingDirectory.getPath(), overwrite: "true", outputencoding: StandardCharsets.UTF_8.name())
            }
        }
    }

    private String[] buildCmdArgs() {
        List<String> args = new ArrayList<>()

        args.add('-o')
        args.add(toolingDirectory.getAbsolutePath())
        args.add('--DeprecatedTopology')
        args.add(String.valueOf(includeDeprecatedTopologyProperties))
        if (runAllGenerators) {
            args.add('--all')
        } else {
            if (generateMonitoringPolicy) {
                args.add('--MonitoringPolicy')
            }

            if (generateAspWrappers) {
                args.add('--AspWrappers')
            }

            if (generateTopology) {
                args.add('--Topology')
            }

            if (generateCDT) {
                args.add('--CDT')
            }

            if (generateUML) {
                args.add('--TopologyUML')
            }

            if (generateAgentManifest) {
                args.add('--AgentManifest')
            }

            if (generateTopologySubmitter) {
                args.add('--TopologySubmitter')
            }

            if (generateCallbackCode) {
                args.add('--CallbackInterface')
            }

            if (generateCollectorInterface) {
                args.add('--DataCollectorInterface')
            }
        }

        args.add(agentDefinition.getAbsolutePath())

        return args.toArray(new String[0])
    }
}
