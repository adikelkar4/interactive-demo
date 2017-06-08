pipeline {
    agent { docker 'maven:3.3.9' }
    triggers { pollSCM('H/3 * * * *') }

    stages {
        stage('dependencies') {
            steps {
                sh 'mvn dependency:resolve '
            }
        }
        stage('maven build') {
            steps {
                sh 'mvn clean install '
            }
        }
    }

    post {
      always {
        archive '*/target/*.jar,*/target/*.war'

        step([$class: 'Mailer',
           notifyEveryUnstableBuild: true,
           recipients: emailextrecipients([[$class: 'CulpritsRecipientProvider'],
                                      [$class: 'RequesterRecipientProvider']])])      }
    }
}