pipeline {
  agent none

  options {
    timestamps()
    ansiColor('xterm')
    disableConcurrentBuilds()
  }

  stages {
    stage('Build (JDK 17)') {
      agent {
        dockerContainer {
          image 'registry.altlinux.org/sisyphus/alt:latest'
        }
      }
      steps {
        sh '''
          apt-get update
          apt-get install -y git ca-certificates curl unzip java-17-openjdk-devel
        '''
        checkout scm
        sh './gradlew publishToMavenLocal'
      }
    }

    stage('Tests') {
      matrix {
        axes {
          axis {
            name 'JAVA'
            values '11', '17'
          }
        }
        agent {
          dockerContainer {
            image 'registry.altlinux.org/sisyphus/alt:latest'
          }
        }
        stages {
          stage('Install build dependencies') {
            steps {
              sh '''
                apt-get update
                if [ "${JAVA}" = "11" ]; then
                  apt-get install -y git ca-certificates curl unzip java-11-openjdk-devel
                else
                  apt-get install -y git ca-certificates curl unzip java-17-openjdk-devel
                fi
                apt-get install -y google-gson maven-lib shadow-gradle-plugin
              '''
            }
          }

          stage('Checkout') {
            steps {
              checkout scm
            }
          }

          stage('Check') {
            steps {
              sh '''
                if [ "${JAVA}" = "11" ]; then
                  ./gradlew check \
                    -Djava.library.dir="/usr/share/java" \
                    -Dmaven.poms.dir="/usr/share/maven-poms" \
                    -Djava11
                else
                  ./gradlew check \
                    -Djava.library.dir="/usr/share/java" \
                    -Dmaven.poms.dir="/usr/share/maven-poms"
                fi
              '''
            }
          }
        }
      }
    }
  }
}
