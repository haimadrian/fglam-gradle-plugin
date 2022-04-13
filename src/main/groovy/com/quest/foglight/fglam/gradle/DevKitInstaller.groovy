package com.quest.foglight.fglam.gradle

import org.gradle.api.Project

/**
 * I created this class to "download and install" dev-kit.<br/>
 * We need dev-kit to be available under build folder, so AgentCompiler (gluetools library) can find
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
    return "${project.buildDir}/fglam/dev-kit"
  }

  /**
   * Download fglam archives and untar them to {@link #getDevKitHomeDir dev-kit home folder}<br/>
   * fglam archives are:
   * <ul>
   *  <li>devkit-common-examples.tar.gz</li>
   *  <li>devkit-common-jars.tar.gz</li>
   *  <li>devkit-native.tar.gz</li>
   * </ul>
   * @param project A project to install fglam dev-kit for. (Under its build folder)
   */
  static void installDevKit(Project project) {
    // TODO: copy fglam archives
    println('Downloading and untarring fglam archives, so we can have devkit libs available for AgentCompiler')
  }
}
