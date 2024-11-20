import java.nio.file.*

def call() {
    // Create the demo-backend folder if it doesn't exist
    jobDsl script: '''
        folder('demo-backend') {
            description('Node.js Backend Applications')
        }
    '''
    
    // Find all directories containing a package.json file
    def apps = findFiles(glob: '**/package.json')
        .findAll { !it.path.contains('node_modules') }
        .collect { it.path.split('/')[0] }
        .unique()
    
    apps.each { app ->
        // Create multibranch pipeline for each app
        jobDsl script: """
            multibranchPipelineJob('demo-backend/${app}') {
                branchSources {
                    git {
                        id('${app}-repo')
                        remote('${scm.getUserRemoteConfigs()[0].getUrl()}')
                        includes('*/main')
                    }
                }
                factory {
                    workflowBranchProjectFactory {
                        scriptPath('${app}/Jenkinsfile')
                    }
                }
                triggers {
                    periodic(1)
                }
            }
        """
    }
    
    // Build the pipelines in parallel
    def parallelStages = [:]
    
    apps.each { app ->
        parallelStages[app] = {
            stage("Build ${app}") {
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
    
    parallel parallelStages
}

return this
