rootProject.name = "cloudburst"

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.4.0")
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

include("api", "server", "vanilla")
