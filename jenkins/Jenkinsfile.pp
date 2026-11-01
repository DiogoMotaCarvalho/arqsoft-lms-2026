pipeline {
  agent none

  options {
    buildDiscarder(logRotator(numToKeepStr: '30'))
    disableConcurrentBuilds()
    copyArtifactPermission('psoft-g1-prod')
  }

  parameters {
    string(name: 'CI_BUILD', defaultValue: '', description: 'Number of the psoft-g1-ci build whose JAR passed DEV.')
  }

  environment {
    CI_JOB = 'psoft-g1-ci'
    RELEASE_APPROVERS = 'release-managers'
  }

  stages {
    stage('Deploy PP') {
      agent any
      steps {
        script {
          if (!(params.CI_BUILD ==~ /[1-9][0-9]*/)) {
            error('CI_BUILD must be a build number.')
          }
        }
        dir('artifact') {
          deleteDir()
          copyArtifacts(projectName: env.CI_JOB, selector: specific(params.CI_BUILD),
            filter: 'validated/app.jar,validated/release.properties', flatten: true,
            fingerprintArtifacts: true)
        }
        script {
          if (!fileExists('artifact/app.jar') || !fileExists('artifact/release.properties')) {
            error('The selected CI build did not publish a JAR validated on DEV.')
          }
          echo readFile('artifact/release.properties')
          currentBuild.description = "CI #${params.CI_BUILD}"
          def deploy = load 'deploy/jenkins-deploy.groovy'
          env.PP_DEPLOYMENT = deploy.call('pp')
        }
        stash name: 'pp-candidate', includes: 'artifact/app.jar,artifact/release.properties'
      }
    }

    stage('PP acceptance') {
      steps {
        script {
          timeout(time: 7, unit: 'DAYS') {
            env.PP_APPROVER = input(
              message: "Accept ${env.CI_JOB} #${params.CI_BUILD}, now on PP, and promote it to PROD?",
              ok: 'Accept and promote',
              submitter: env.RELEASE_APPROVERS,
              submitterParameter: 'APPROVER')
          }
        }
      }
    }

    stage('Archive accepted artifact') {
      agent any
      steps {
        dir('artifact') { deleteDir() }
        unstash 'pp-candidate'
        script {
          lock(resource: 'psoft-g1-pp') {
            sshagent(credentials: ['psoft-deploy-ssh']) {
              timeout(time: 3, unit: 'MINUTES') {
                def deploy = load 'deploy/jenkins-deploy.groovy'
                deploy.verify('pp', env.PP_DEPLOYMENT)
              }
            }
            dir('accepted') { deleteDir() }
            sh 'mkdir -p accepted && cp artifact/app.jar artifact/release.properties accepted/'
            writeFile file: 'accepted/acceptance.properties', text: "CI_JOB=${env.CI_JOB}\nCI_BUILD=${params.CI_BUILD}\nPP_JOB=${env.JOB_NAME}\nPP_BUILD=${env.BUILD_NUMBER}\nACCEPTED_BY=${env.PP_APPROVER}\n"
            archiveArtifacts artifacts: 'accepted/app.jar,accepted/release.properties,accepted/acceptance.properties', fingerprint: true
          }
        }
      }
    }

    stage('Promote to PROD') {
      steps {
        build job: 'psoft-g1-prod', parameters: [string(name: 'PP_BUILD', value: env.BUILD_NUMBER)], wait: false
      }
    }
  }
}
