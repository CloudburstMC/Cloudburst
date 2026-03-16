publishing {
    publications {
        withType<MavenPublication> {
            artifactId = "cloudburst-vanilla"
        }
    }
}

dependencies {
    compileOnly(projects.api)
}
