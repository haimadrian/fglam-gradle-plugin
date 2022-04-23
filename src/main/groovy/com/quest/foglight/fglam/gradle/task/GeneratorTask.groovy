package com.quest.foglight.fglam.gradle.task

import com.quest.foglight.fglam.gradle.DevKitInstaller
import com.quest.glue.tools.AgentCompiler
import com.quest.glue.tools.compiler.CallbackCodeGenerator
import com.quest.glue.tools.compiler.CodeGenerator
import com.quest.glue.tools.compiler.cdt.CDTGenerator
import com.quest.glue.tools.compiler.datacollector.CollectorInterfaceGenerator
import com.quest.glue.tools.compiler.manifest.ManifestGenerator
import com.quest.glue.tools.compiler.property.asps.ASPServiceGenerator
import com.quest.glue.tools.compiler.property.fmsconfig.MonitoringPolicyGenerator
import com.quest.glue.tools.compiler.topology.TopologyTemplateGenerator
import com.quest.glue.tools.compiler.topology.TopologyUMLGenerator
import com.quest.glue.tools.compiler.topology.TopologyXMLGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat

/**
 * A Gradle task used for generating topology classes based on agent-definition and options.<br/>
 * The task relies on gluetools module. See {@link com.quest.glue.tools.AgentCompiler}.<br/>
 * As a prior step, the task will copy a specified {@link GeneratorTask#agentDefinition agent-definition.xml} file
 * to {@link GeneratorTask#toolingDirectory tooling} directory, and update it, by replacing @product.version@ token
 * with the value of {@code project.foglight.branch} property, from foglight-gradle-plugin. In addition, we copy all
 * sibling files of the specified agent-definition.xml, to tooling directory. Then we let AgentCompiler to generate sources
 * using the tooling directory as its input.<br/>
 * So you must make sure your project configuration folder is in correct structure, having the agent-definition.xml file
 * and all of its dependencies. (files like base-topology.xml for example). It is your responsibility to prepare the
 * config folder before calling GeneratorTask. Otherwise AgentCompiler will fail or have an unexpected outcome.
 *
 * @author Haim Adrian
 * @since 13-Apr-2022
 * @see GeneratorTask#agentDefinition
 * @see GeneratorTask#runAllGenerators
 * @see GeneratorTask#generateCDT
 * @see GeneratorTask#generateTopology
 * @see GeneratorTask#generateUML
 * @see GeneratorTask#includeDeprecatedTopologyProperties
 * @see GeneratorTask#buildNumber
 * @see GeneratorTask#generateMonitoringPolicy
 * @see GeneratorTask#generateAspWrappers
 * @see GeneratorTask#generateAgentManifest
 * @see GeneratorTask#generateTopologySubmitter
 * @see GeneratorTask#generateCallbackCode
 * @see GeneratorTask#generateCollectorInterface
 * @see GeneratorTask#toolingDirectory
 */
class GeneratorTask extends DefaultTask {
    private static final String FGLAM_TOOLING_CART_BUILD_ID_PROPERTY = "fglam.tooling.cart.buildId"
    private static final String BUILD_TIMESTAMP = new SimpleDateFormat("yyyyMMdd-HHmm").format(new Date())

    /** A reference to agent-definition.xml file to be used for generating topology classes.
     * If the file is not under {@link #toolingDirectory}, we will copy it with its siblings, and
     * update agent definition according to project version. (Replacing product.version token)
     * @see GeneratorTask */
    @InputFile
    File agentDefinition

    /** Whether to run all agent generators or not.<br/>
     * <b>Default</b> value is: <code>true</code>
     * @see GeneratorTask */
    @Optional
    @Input
    Boolean runAllGenerators = true

    /** <b>Default</b> value is: <code>false</code>
     * @see GeneratorTask
     * @see com.quest.glue.tools.compiler.cdt.CDTGenerator */
    @Optional
    @Input
    Boolean generateCDT = false

    /** <b>Default</b> value is: <code>false</code>
     * @see GeneratorTask
     * @see com.quest.glue.tools.compiler.topology.TopologyXMLGenerator */
    @Optional
    @Input
    Boolean generateTopology = false

    /** <b>Default</b> value is: <code>false</code>
     * @see GeneratorTask
     * @see com.quest.glue.tools.compiler.topology.TopologyUMLGenerator */
    @Optional
    @Input
    Boolean generateUML = false

    /** Whether to include deprecated topology properties or not<br/>
     * <b>Default</b> value is: <code>true</code>
     * @see GeneratorTask */
    @Optional
    @Input
    Boolean includeDeprecatedTopologyProperties = true

    /** For backward compatability.<br/>
     * <b>Default</b> value is: <code>VERSION-TIMESTAMP</code> e.g <code>6.3.0-20220413-1155</code>
     * @see GeneratorTask */
    @Optional
    @Input
    String buildNumber = null

    /** For backward compatability.<br/>
     * <b>Default</b> value is: <code>false</code>
     * @see GeneratorTask
     * @see com.quest.glue.tools.compiler.property.fmsconfig.MonitoringPolicyGenerator */
    @Optional
    @Input
    Boolean generateMonitoringPolicy = false

    /** For backward compatability.<br/>
     * <b>Default</b> value is: <code>false</code>
     * @see GeneratorTask
     * @see com.quest.glue.tools.compiler.property.asps.ASPServiceGenerator */
    @Optional
    @Input
    Boolean generateAspWrappers = false

    /** For backward compatability.<br/>
     * <b>Default</b> value is: <code>false</code>
     * @see GeneratorTask
     * @see com.quest.glue.tools.compiler.manifest.ManifestGenerator */
    @Optional
    @Input
    Boolean generateAgentManifest = false

    /** For backward compatability.<br/>
     * <b>Default</b> value is: <code>false</code>
     * @see GeneratorTask
     * @see com.quest.glue.tools.compiler.topology.TopologyTemplateGenerator */
    @Optional
    @Input
    Boolean generateTopologySubmitter = false

    /** For backward compatability.<br/>
     * <b>Default</b> value is: <code>false</code>
     * @see GeneratorTask
     * @see com.quest.glue.tools.compiler.CallbackCodeGenerator */
    @Optional
    @Input
    Boolean generateCallbackCode = false

    /** For backward compatability.<br/>
     * <b>Default</b> value is: <code>false</code>
     * @see GeneratorTask
     * @see com.quest.glue.tools.compiler.datacollector.CollectorInterfaceGenerator */
    @Optional
    @Input
    Boolean generateCollectorInterface = false

    /** Output directory, where generated files will be found.<br/>
     * <b>Default</b> value is: <code>build/tooling</code>
     * @see GeneratorTask */
    @OutputDirectory
    File toolingDirectory

    /**
     * Constructs a new {@link GeneratorTask}
     */
    GeneratorTask() {
        setGroup('build')
        setDescription('Generate topology classes, callbacks, agent.manifest, etc. based on agent definitions')
    }

    @Override
    Task configure(Closure closure) {
        // First call super, so we will have agentDefinition available
        super.configure(closure)

        dependsOn getProject().tasks.copyDependencies

        toolingDirectory = new File(getProject().buildDir, 'tooling').getAbsoluteFile()

        inputs.dir(agentDefinition.getParentFile())
        outputs.file(toolingDirectory)

        println "GeneratorTask input: ${agentDefinition.getParentFile()}"
        println "GeneratorTask output: $toolingDirectory"

        // Eventually call super again, to let user to override the defaults
        return super.configure(closure)
    }

    @TaskAction
    def generator() {
        if ((agentDefinition == null) || !agentDefinition.exists()) {
            println 'You must specify agentDefinition.. Like.. what am I supposed to do?'
            throw new FileNotFoundException('NOT_FOUND: agent-definition.xml. Please specify agentDefinition property and make sure file exists.')
        }

        if (toolingDirectory == null) {
            toolingDirectory = new File(getProject().buildDir, 'tooling')
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
            buildNumber = "${getProject().foglight.version.get()}-$BUILD_TIMESTAMP"
        }

        System.setProperty(FGLAM_TOOLING_CART_BUILD_ID_PROPERTY, buildNumber)
        System.setProperty('quest.home', DevKitInstaller.getDevKitHomeDir(project))

        def cmdArgs = buildCmdArgs()

        resetAgentCompiler()

        println "Running AgentCompiler with arguments: ${Arrays.toString(cmdArgs)}"
        def responseCode = AgentCompiler.internalMain(cmdArgs)
        println "Response code of AgentCompiler is: $responseCode"

        if (responseCode != 0) {
            throw new GradleException("AgentCompiler failed to generate sources. Check console for more information. ResponseCode=$responseCode")
        }
    }

    private void optionallyUpdateAgentDefinition() {
        if (!agentDefinition.getParent().equalsIgnoreCase(toolingDirectory.getPath())) {
            println "Updating agent-definition.xml and copy it with its sibling files to $toolingDirectory"

            getProject().ant.copy(file: agentDefinition.getPath(), todir: toolingDirectory.getPath(), overwrite: "true", outputencoding: StandardCharsets.UTF_8.name()) {
                filterset {
                    filter(token: "product.version", value: getProject().foglight.branch.get())
                }
            }

            def siblings = agentDefinition.getParentFile().listFiles(new FilenameFilter() {
                boolean accept(File dir, String name) {
                    return name != agentDefinition.getName()
                }
            })

            siblings.each { sibling ->
                getProject().ant.copy(file: sibling.getPath(), todir: toolingDirectory.getPath(), overwrite: "true", outputencoding: StandardCharsets.UTF_8.name())
            }

            // Now that all definition files are under tooling directory, modify agentDefinition reference to look at the up-to-date file
            agentDefinition = new File(toolingDirectory, agentDefinition.getName())
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

    /**
     * AgentCompiler contains GENERATORS static map. When we call AgentCompiler twice,
     * we fail because of this static (singleton) map...............<br/>
     * As a result, I had to implement some disgusting reset...</br>
     * This is an evil idea, yet I'm not going to mess with Glue/Core/Whatever
     */
    private static void resetAgentCompiler() {
        Map<String, CodeGenerator> GENERATORS = new HashMap<>(9)
        GENERATORS.put("CDT", new CDTGenerator())
        GENERATORS.put("Topology", new TopologyXMLGenerator())
        GENERATORS.put("TopologySubmitter", new TopologyTemplateGenerator())
        GENERATORS.put("TopologyUML", new TopologyUMLGenerator())
        GENERATORS.put("AspWrappers", new ASPServiceGenerator())
        GENERATORS.put("MonitoringPolicy", new MonitoringPolicyGenerator())
        GENERATORS.put("AgentManifest", new ManifestGenerator())
        GENERATORS.put("CallbackInterface", new CallbackCodeGenerator())
        GENERATORS.put("DataCollectorInterface", new CollectorInterfaceGenerator())

        Field generatorsField = AgentCompiler.class.getDeclaredField('GENERATORS')
        generatorsField.setAccessible(true)

        // Make the final writable
        Field modifiersField = Field.class.getDeclaredField("modifiers")
        modifiersField.setAccessible(true)
        modifiersField.setInt(generatorsField, generatorsField.getModifiers() & ~Modifier.FINAL)

        generatorsField.set(null, GENERATORS)

        // Return to be final
        modifiersField.setInt(generatorsField, generatorsField.getModifiers() & Modifier.FINAL)
    }
}
