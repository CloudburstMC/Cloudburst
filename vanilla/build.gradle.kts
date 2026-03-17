publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "cloudburst-vanilla"
            from(components["java"])
        }
    }
}

dependencies {
    compileOnly(projects.api)
}
