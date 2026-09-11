plugins {
    alias(libs.plugins.extra.java.module.info)
}

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
    api(libs.brigadier)
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

extraJavaModuleInfo {
    failOnAutomaticModules.set(false)
    module(libs.adventure.api, "net.kyori.adventure")
    module("net.kyori:adventure-key", "net.kyori.adventure.key")
    knownModule("net.kyori:examination-api", "net.kyori.examination.api")
    knownModule("net.kyori:examination-string", "net.kyori.examination.string")
    knownModule("org.jetbrains:annotations", "org.jetbrains.annotations")
    module(libs.brigadier, "com.mojang.brigadier") {
        overrideModuleName()
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module(libs.guice, "com.google.guice")
    knownModule(libs.guava, "com.google.common")
    knownModule(libs.jakarta.inject, "jakarta.inject")
    module(libs.math.immutable, "org.cloudburstmc.math.immutable") {
        overrideModuleName()
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    automaticModule("aopalliance:aopalliance", "aopalliance.aop")
    automaticModule("com.google.guava:listenablefuture", "com.google.guava.listenablefuture")
}
