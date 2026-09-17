plugins {
    application
}

application {
    mainClass.set("org.cloudburstmc.codegen.VanillaDataGen")
}

tasks.register<JavaExec>("generateVanillaData") {
    group = "codegen"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.cloudburstmc.codegen.VanillaDataGen")
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.javapoet)
}
