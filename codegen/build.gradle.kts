plugins {
    application
}

application {
    mainClass.set("org.cloudburstmc.codegen.BlockDataGen")
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.javapoet)
}
