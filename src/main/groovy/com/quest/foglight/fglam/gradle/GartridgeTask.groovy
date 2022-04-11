package com.quest.foglight.fglam.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GartridgeTask extends DefaultTask {

    @Input
    String carName

    @Input
    String carVersion

    @Input
    Boolean buildLocalizableCar = true

    @OutputDirectory
    File outputDir

    @Internal
    Closure content

    public content(Closure content) {
        this.content = content
    }

    @TaskAction
    def createCar() {
        def fglAntDir = new File(project.rootProject.buildDir, "fglant")
        if (!fglAntDir.exists()) {
            project.rootProject.configurations.fglAntZip.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                project.copy {
                    from project.rootProject.zipTree(artifact.getFile())
                    into fglAntDir
                }
            }
        }

        ant.taskdef(name: 'car', classname: 'com.quest.nitro.tools.ant.cartridge.Car',
            classpath: project.rootProject.fileTree(dir: fglAntDir).asPath)

        ant.car([file: "${carName}.car", destDir: outputDir,
                 buildLocalizableCar: buildLocalizableCar], content)
    }
}
