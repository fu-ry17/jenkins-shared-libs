import java.nio.file.*

def call() {
    def repositoryURL = scm.getUserRemoteConfigs()[0].getUrl()
    
    // Get repository owner and name from the URL
    def matcher = repositoryURL =~ /.+[\/:](?<owner>[^\/]+)\/(?<repository>[^\/]+)\.git$/
    matcher.matches()
    String repositoryOwner = matcher.group('owner')
    String repositoryName = matcher.group('repository')

    // Discovery strategies
    final int EXCLUDE_PULL_REQUESTS_STRATEGY_ID = 1
    final int USE_CURRENT_SOURCE_STRATEGY_ID = 2

    // Create folder for the monorepo
    folder('demo-backend')

    // Automatically detect apps by looking for package.json files
    def apps = findApps()

    apps.each { app ->
        multibranchPipelineJob("demo-backend/${app}") {
            branchSources {
                branchSource {
                    source {
                        github {
                            id("github-${app}")
                            repoOwner(repositoryOwner)
                            repository(repositoryName)
                            repositoryUrl(repositoryURL)
                            configuredByUrl(false)
                            credentialsId('github_credentials')

                            traits {
                                // Sparse checkout for monorepo
                                sparseCheckoutPathsTrait {
                                    extension {
                                        sparseCheckoutPaths {
                                            sparseCheckoutPath {
                                                path("${app}/*")
                                            }
                                        }
                                    }
                                }

                                // Branch discovery
                                gitHubBranchDiscovery {
                                    strategyId(EXCLUDE_PULL_REQUESTS_STRATEGY_ID)
                                }

                                // PR discovery
                                gitHubPullRequestDiscovery {
                                    strategyId(USE_CURRENT_SOURCE_STRATEGY_ID)
                                }

                                // Path filtering
                                pathRestrictionFilter {
                                    includePaths("${app}/*")
                                }

                                // Custom notification context
                                notificationContextTrait {
                                    contextLabel("ci/jenkins/${app}")
                                    typeSuffix(false)
                                }
                            }
                        }
                    }

                    // Build strategies
                    buildStrategies {
                        skipInitialBuildOnFirstBranchIndexing()
                        changeRequestBuildStrategy {
                            ignoreTargetOnlyChanges(true)
                        }
                    }
                }
            }

            // Configure the Jenkinsfile path for each app
            factory {
                workflowBranchProjectFactory {
                    scriptPath("${app}/Jenkinsfile")
                }
            }

            // Cleanup old items
            orphanedItemStrategy {
                discardOldItems {
                    daysToKeep(7)
                    numToKeep(10)
                }
            }

            // Periodic scanning
            triggers {
                periodicFolderTrigger {
                    interval('15m')
                }
            }

            // Configure job properties
            configure { node ->
                def properties = node / properties
                properties << 'jenkins.branch.NoTriggerBranchProperty' {
                    branches ''
                }
            }
        }
    }
}

def findApps() {
    // Find all directories containing a package.json file
    def output = sh(script: 'find . -name "package.json" -not -path "*/node_modules/*" -not -path "./package.json"', returnStdout: true).trim()
    
    // Convert the output to a list of app names
    return output.split('\n')
        .findAll { it.contains('/package.json') }
        .collect { it.split('/')[1] }
        .findAll { it != 'node_modules' }
}

return this