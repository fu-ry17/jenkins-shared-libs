import java.nio.file.*

def call() {
    pipeline {
        agent any
        stages {
            stage('Scan Projects') {
                steps {
                    script {
                        // Find all directories containing a package.json file
                        def apps = findFiles(glob: '**/package.json')
                            .findAll { !it.path.contains('node_modules') }
                            .collect { it.path.split('/')[0] }
                            .unique()
                        
                        apps.each { app ->
                            echo "Found application: ${app}"
                            if (fileExists("${app}/Jenkinsfile")) {
                                dir(app) {
                                    echo "Building ${app}"
                                    load 'Jenkinsfile'
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

return this
