plugins {
    application
}

application {
    mainClass.set("org.cloudburstmc.codegen.ItemDataGen")
}

tasks.register<JavaExec>("generateItemData") {
    group = "codegen"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.cloudburstmc.codegen.ItemDataGen")
}

tasks.register<JavaExec>("generateBlockData") {
    group = "codegen"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.cloudburstmc.codegen.BlockDataGen")
}

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.javapoet)
}
