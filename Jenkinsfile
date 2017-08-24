
// To be used as both a tag (for a series of builds) and a prefix to
// identify the push from a single build.

def tag_prefix=env.BRANCH_NAME.replaceAll(/[^a-zA-Z0-9_]/,"_")
aws_credentials='interactive-demo-manager'
aws_region='us-east-2'
expiration="2"
cluster_user="build-${BUILD_NUMBER}"

// We're currently building all steps on a single node, labeled 'aml'

node('aml') {
   stage('checkout') {
      checkout scm
   }

   // Perform the maven build inside the 'maven:3.3.9' docker image,
   // so all dependencies are solved in the maven build area.

   docker.image("maven:3.3.9").inside {
     stage('Maven Build') {
       sh 'mvn clean install -Dmaven.repo.local=./maven-local'
     }
     stage('Archiving') {
       archive '*/target/*.jar,*/target/*.war'
     }
   }

   // Surround all docker push activity with credentials
   withDockerRegistry([credentialsId: 'docker.io-nuodb-push']) {
     def web
     def user

     stage('docker build') {
       dir("StorefrontUser") {
         user = docker.build "nuodb/storefrontuser-demo"
       }
       dir("StorefrontWeb") {
         web = docker.build "nuodb/storefrontweb-demo"
       }
     }
     stage('docker push') {
       user.push("${tag_prefix}-${BUILD_NUMBER}")
       web.push("${tag_prefix}-${BUILD_NUMBER}")
       user.push("${tag_prefix}")
       web.push("${tag_prefix}")

       if(env.BRANCH_NAME.equals("release")) {
         user.push("latest")
         web.push("latest")
       }
     }
  }

  // TODO:  Ensure that this cluster is actually using the images we just built
    stage('Cluster Create') {
      docker.image("python:2.7").inside {
        withEnv(["AWS_DEFAULT_REGION=${aws_region}", "USER=${cluster_user}"]) {
          withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: aws_credentials, secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            sh "virtualenv py27 && . py27/bin/activate && pip install -r requirements.txt"
            sh ". py27/bin/activate && bin/cluster create --delete-after ${expiration} | tee create-output.txt"
            output=readFile('create-output.txt').trim()
            url=(output =~ /(http.*)/)[0][1]
          }
        }
      }
    }

    stage('Cluster Verify') {
       sh "curl ${url}"
       // TODO:  We need some kind of 'self check' URL to call
    }
   
   stage('Cluster Delete') {
      docker.image("python:2.7").inside {
        withEnv(["AWS_DEFAULT_REGION=${aws_region}", "USER=${cluster_user}"]) {
	  withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', accessKeyVariable: 'AWS_ACCESS_KEY_ID', credentialsId: aws_credentials, secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
            sh "virtualenv py27 && . py27/bin/activate && pip install -r requirements.txt"
 	    sh ". py27/bin/activate && bin/cluster delete --include ${cluster_user}-"
          }
        }
   }
}
