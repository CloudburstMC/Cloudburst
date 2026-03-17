publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "cloudburst-api"
            from(components["java"])
        }
    }
}

dependencies {
    api(libs.adventure.api)
    api(libs.adventure.text.minimessage)
    api(libs.adventure.text.serializer.legacy)
    api(libs.slf4j.api)
    api(libs.jackson.core)
    api(libs.jackson.annotations)
    api(libs.jackson.databind)
    api(libs.jackson.dataformat.yaml)
    api(libs.jackson.dataformat.properties)
    api(libs.completable.futures)
    api(libs.math.immutable)
    api(libs.guice)
    api(libs.guava)
    api(libs.asm)
    api(libs.asm.commons)
    api(libs.jakarta.inject)
}
