node('aml') {
   stage 'Maven'
   docker.image("maven:3.3.9").inside {
       sh 'mvn clean install '
       archive '*/target/*.jar,*/target/*.war'
   }

   stage 'docker build'
   echo "Branch name is ${BRANCH_NAME}"

   withDockerRegistry([credentialsId: 'docker.io-nuodb-push', url: 'https://index.docker.io/v1/']) {
     dir("StorefrontUser") {
       def user = docker.build "nuodb/storefrontuser-demo"
       user.push("${JOB_BASE_NAME}-${BUILD_NUMBER}")
     }
     dir("StorefrontWeb") {
       def web = docker.build "nuodb/storefrontweb-demo"
       web.push("${JOB_BASE_NAME}-${BUILD_NUMBER}")
     }
   }
}
