pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'Java 8'
    }
    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '5'))
    }
    stages {
        stage ('Build') {
            when { not { anyOf {
                branch 'stable'
                branch 'beta'
                branch 'bleeding'
            }}}

            steps {
                sh 'mvn clean package'
            }
        }
        stage ('Deploy') {
            when {
                anyOf {
                    branch 'stable'
                    branch 'beta'
                    branch 'bleeding'
                }
            }

            stages {
                stage('Setup') {
                    steps {
                        rtMavenDeployer(
                                id: "maven-deployer",
                                serverId: "opencollab-artifactory",
                                releaseRepo: "maven-releases",
                                snapshotRepo: "maven-snapshots"
                        )
                        rtMavenResolver(
                                id: "maven-resolver",
                                serverId: "opencollab-artifactory",
                                releaseRepo: "release",
                                snapshotRepo: "snapshot"
                        )
                    }
                }

                stage('Release') {
                    when {
                        anyOf {
                            branch 'stable'
                            branch 'beta'
                        }
                    }

                    steps {
                        rtMavenRun(
                                pom: 'pom.xml',
                                goals: 'javadoc:jar source:jar install',
                                deployerId: "maven-deployer",
                                resolverId: "maven-resolver"
                        )
                        sh 'mvn javadoc:javadoc -DskipTests'
                        step([$class: 'JavadocArchiver', javadocDir: 'target/site/apidocs', keepAll: false])
                    }
                }

                stage('Snapshot') {
                    when {
                        anyOf {
                            branch 'bleeding'
                        }
                    }
                    steps {
                        rtMavenRun(
                                pom: 'pom.xml',
                                goals: 'javadoc:jar source:jar install',
                                deployerId: "maven-deployer",
                                resolverId: "maven-resolver"
                        )
                    }
                }

                stage('Publish') {
                    steps {
                        rtPublishBuildInfo(
                                serverId: "opencollab-artifactory"
                        )
                    }
                }
            }
        }
    }

    post {
        success {
            junit 'target/surefire-reports/**/*.xml'
            archiveArtifacts artifacts: 'target/Cloudburst.jar', fingerprint: true
        }
        always {
            deleteDir()
        }
    }
}