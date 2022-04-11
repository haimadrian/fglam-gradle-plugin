@Library('foglight') _

pipeline {
  agent {
    node {
       label 'fve-repo'
    }
  }

  options {
    // Show timestamps in Jenkins console
    timestamps()
  }
  
  stages {
    stage("Build Only") {
      when {
        not {
          anyOf {
            branch 'master'; branch 'release/**'
          }
        }
      }
      steps {
        doGradle("build")
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/test-results/test/*.xml'
        }
      }
    }

    stage("Build & Publish") {
      when {
        anyOf {
          branch 'master'; branch 'release/**'
        }
      }
      steps {
        doGradle("publish")
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/test-results/test/*.xml'
        }
      }
    }

    stage("Artifacts Archiver") {
      steps {
        archiveArtifacts artifacts: '**/*.jar', excludes: '**/libs/**', fingerprint: true
      }
    }
  }
  
  post {
    always {
      cleanWs()
      sendNotification()
    }
  }
}
