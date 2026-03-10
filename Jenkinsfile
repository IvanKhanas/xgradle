pipeline {
  agent none

  options {
    timestamps()
    ansiColor('xterm')
    disableConcurrentBuilds()
    skipDefaultCheckout(true)
  }

  stages {
    stage('Checkout') {
      agent any
      steps {
        checkout scm
        stash name: 'src', includes: '**/*', useDefaultExcludes: false
      }
    }

    stage('Checkstyle') {
      agent {
        dockerContainer {
          image 'xenoalt/alt-sisyphus-java:1.0.0-j21'
        }
      }
      steps {
        sh '''
          apt-get update
          apt-get install -y \
            git \
            java-11-openjdk-devel
        '''
        deleteDir()
        unstash 'src'
        sh './gradlew checkstyle -Djava11'
      }
      post {
        always {
          archiveArtifacts artifacts: '**/build/reports/checkstyle/**', allowEmptyArchive: true
        }
      }
    }

    stage('Build (JDK 11)') {
      agent {
        dockerContainer {
          image 'xenoalt/alt-sisyphus-java:1.0.0-j21'
        }
      }
      steps {
        sh '''
          apt-get update
        '''
        deleteDir()
        unstash 'src'
        sh './gradlew publishToMavenLocal -Djava11'
      }
    }

    stage('Build (JDK 17+)') {
      agent {
        dockerContainer {
          image 'xenoalt/alt-sisyphus-java:1.0.0-j21'
        }
      }
      steps {
        sh '''
          apt-get update
        '''
        deleteDir()
        unstash 'src'
        sh './gradlew publishToMavenLocal'
      }
    }

    stage('Tests (JDK 11)') {
      agent {
        dockerContainer {
          image 'xenoalt/alt-sisyphus-java:1.0.0-j21'
        }
      }
      steps {
        sh '''
          apt-get update
          apt-get install -y \
            google-gson \
            maven-lib \
            shadow-gradle-plugin
        '''
        deleteDir()
        unstash 'src'
        sh '''
          ./gradlew build \
            -Djava11 \
            --info
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
        }
      }
    }

    stage('Tests (JDK 17+)') {
      agent {
        dockerContainer {
          image 'xenoalt/alt-sisyphus-java:1.0.0-j21'
        }
      }
      steps {
        sh '''
          apt-get update
          apt-get install -y \
            google-gson \
            apache-commons-io \
            apache-commons-cli \
            shadow-gradle-plugin
        '''
        deleteDir()
        unstash 'src'
        sh '''
          ./gradlew build \
            --info
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
        }
      }
    }

    stage('JaCoCo Report') {
      agent {
        dockerContainer {
          image 'xenoalt/alt-sisyphus-java:1.0.0-j21'
        }
      }
      steps {
        sh '''
          apt-get update
          apt-get install -y \
            google-gson \
            apache-commons-io \
            apache-commons-cli \
            shadow-gradle-plugin
        '''
        deleteDir()
        unstash 'src'
        sh '''
          ./gradlew test jacocoTestReport \
            --info
        '''
      }
      post {
        always {
          junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
          archiveArtifacts artifacts: '**/build/reports/jacoco/**', allowEmptyArchive: true
          archiveArtifacts artifacts: '**/build/reports/tests/**', allowEmptyArchive: true
        }
      }
    }
  }
}