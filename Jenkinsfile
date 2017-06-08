pipeline {
    agent { docker 'maven:3.3.9' }
    stages {
        stage('checkout') {
    	  steps {
  	    checkout scm
	  }
	}
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
}