def call(Map config = [:]) {
    pipeline {
        agent any
        
        tools {
            nodejs "Node"
        }
        
        environment {
            APP_NAME = getAppName()
        }
        
        stages {
            stage('Setup Node.js') {
                steps {
                    script {
                        sh '''
                            npm install -g pnpm
                            pnpm --version
                            node --version
                        '''
                    }
                }
            }
            
            stage('Install Dependencies') {
                steps {
                    sh 'pnpm install --frozen-lockfile'
                }
            }
            
            stage('Unit Tests') {
                steps {
                    sh 'pnpm run test:unit'
                }
            }
            
            stage('API Tests') {
                steps {
                    sh 'pnpm run test:api'
                }
            }
            
            stage('Build') {
                steps {
                    sh 'pnpm run build || true'
                }
            }
            
            stage('Archive Artifacts') {
                steps {
                    archiveArtifacts artifacts: 'dist/**/*', allowEmptyArchive: true
                }
            }
            
            stage('Coverage Report') {
                steps {
                    sh 'pnpm run coverage || true'
                }
            }
        }
        
        post {
            always {
                junit testResults: '**/junit.xml', allowEmptyResults: true
                publishHTML(target: [
                    allowMissing: true,
                    alwaysLinkToLastBuild: false,
                    keepAll: true,
                    reportDir: 'coverage',
                    reportFiles: 'index.html',
                    reportName: 'Coverage Report'
                ])
                cleanWs()
            }
        }
    }
}

def getAppName() {
    def packageJson = readJSON file: 'package.json'
    return packageJson.name
}