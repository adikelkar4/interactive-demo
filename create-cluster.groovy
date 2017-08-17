// Purpose: this is a build job that creates a demo cluster for customer usage

branch=env.BRANCH
aws_credentials='interactive-demo-manager'
aws_region='us-east-2'
git_repo=env.REPO

node('aml') {
    stage('checkout') {
       checkout([$class: 'GitSCM', branches: [[name: "*/${branch}"]], 
       			  userRemoteConfigs: [[credentialsId: 'nuodb-jenkins.github.com', url: git_repo]]])
    }

    stage('Create') {
      docker.image("python:2.7").inside {
        withEnv(["AWS_DEFAULT_REGION=${aws_region}", "USER=${env.USER}"]) {
	  withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: aws_credentials, secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            sh "virtualenv py27 && . py27/bin/activate && pip install -r requirements.txt"
 	    sh ". py27/bin/activate && bin/cluster create --delete-after ${env.EXPIRATION} && bin/cluster list"
          }
        }
      }
    }
}