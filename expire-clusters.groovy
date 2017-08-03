// Purpose: this is a build job that creates a demo cluster for customer usage

branch="release"
aws_credentials='interactive-demo-manager'
aws_region='us-east-2'

node('aml') {
    stage('checkout') {
        checkout scm
    }

    stage('Purging Clusters') {
      docker.image("python:2.7").inside {
        withEnv(["AWS_DEFAULT_REGION=${aws_region}", "USER=jenkins"]) {
	  withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: aws_credentials, secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            sh "virtualenv py27 && . py27/bin/activate && pip install -r requirements.txt"
 	    sh ". py27/bin/activate && bin/cluster --parallel 10 delete --expired --include demo"
          }
        }
      }
    }
}