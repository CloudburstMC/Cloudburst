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

dependencies {
    implementation(libs.jackson.databind)
    implementation(libs.javapoet)
}
