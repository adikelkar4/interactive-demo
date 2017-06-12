pipeline {
    agent none
    triggers { pollSCM('H/3 * * * *') }

    stages {
        stage('maven build') {
	    agent { docker 'maven:3.3.9' }
            steps {
                sh 'mvn clean install '
            }
            post {
             always {
               archive '*/target/*.jar,*/target/*.war'
	       }
           }
        }
	stage('docker build') {
	   agent { label 'aml' }
	   steps {
              sh "docker build StorefrontUser"
              sh "docker build StorefrontWeb"
           }
	}
    }

    post {
      always {

       step([$class: 'Mailer',
           notifyEveryUnstableBuild: true,
           recipients: emailextrecipients([[$class: 'CulpritsRecipientProvider'],
                                      [$class: 'RequesterRecipientProvider']])])      
      }
    }
}