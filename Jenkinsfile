def tag_prefix=env.BRANCH_NAME.replaceAll(/[^a-zA-Z0-9_]/,"_")

node('aml') {
   stage('checkout') {
      checkout scm
   }
   docker.image("maven:3.3.9").inside {
     stage('Downloading Dependencies') {
       sh 'mvn dependency:resolve -Dmaven.repo.local=./maven-local'
     }
     stage('Maven Build') {
       sh 'mvn clean install -Dmaven.repo.local=./maven-local'
     }
     stage('Archiving') {
       archive '*/target/*.jar,*/target/*.war'
     }
   }

   echo "Branch name is ${BRANCH_NAME}"

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
}
