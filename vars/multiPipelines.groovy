def call() {
    // Load the implementation from resources
    def multiPipelinesImpl = libraryResource 'multiPipelines.groovy'
    // Create a temporary file
    writeFile file: 'multiPipelines.groovy', text: multiPipelinesImpl
    // Load and execute the implementation
    def implementation = load 'multiPipelines.groovy'
    return implementation.call()
} 
