import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    alias(libs.plugins.extra.java.module.info)
    alias(libs.plugins.git.properties)
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":api"))
    implementation(libs.bedrock.codec)
    api(libs.bedrock.connection) {
        exclude("com.nukkitx.fastutil")
    }
    api(libs.bedrock.adventure)
    implementation(libs.netty.transport.nethernet)
    runtimeOnly(libs.libdatachannel.natives) {
        exclude(group = "dev.opencollab", module = "libdatachannel-java")
    }
    implementation(libs.adventure.text.logger.slf4j)
    implementation(libs.adventure.text.serializer.plain)
    api(libs.block.state.updater)
    api(libs.bundles.fastutil)
    api(libs.leveldb.mcpe.jni)
    api(libs.noise)

    compileOnly(libs.spotbugs.annotations)

    implementation(libs.terminal.console.appender)
    implementation(libs.jline.terminal)
    implementation(libs.jline.reader)
    runtimeOnly(libs.jline.terminal.ffm)
    runtimeOnly(libs.jline.terminal.jni)

    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j2.impl)
    runtimeOnly(libs.disruptor)
    implementation(libs.jopt.simple)
    implementation(libs.jose.jwt)
    implementation(libs.upnp)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

extraJavaModuleInfo {
    failOnAutomaticModules.set(false)
    automaticModule(libs.block.state.updater, "org.cloudburstmc.blockstateupdater") {
        overrideModuleName()
    }
    module(libs.brigadier, "com.mojang.brigadier") {
        overrideModuleName()
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    module(libs.math.immutable, "org.cloudburstmc.math.immutable") {
        overrideModuleName()
        exportAllPackages()
        requireAllDefinedDependencies()
    }
    automaticModule(libs.noise, "net.daporkchop.lib.noise")
    automaticModule(libs.upnp, "org.cloudburstmc.upnp")
    automaticModule("dev.opencollab:libdatachannel-java", "tel.schich.libdatachannel")
    automaticModule(libs.libdatachannel.natives, "tel.schich.libdatachannel.natives")
    automaticModule("net.daporkchop.lib:math", "net.daporkchop.lib.math")
    automaticModule("net.daporkchop.lib:common", "net.daporkchop.lib.common")
    automaticModule("net.daporkchop.lib:unsafe", "net.daporkchop.lib.unsafe")
    automaticModule("net.daporkchop.lib:random", "net.daporkchop.lib.random")
    automaticModule("net.daporkchop:leveldb-mcpe-jni", "net.daporkchop.leveldb.mcpe.jni")
    automaticModule("net.sf.jopt-simple:jopt-simple", "net.sf.jopt.simple")
    automaticModule("org.iq80.leveldb:leveldb", "org.iq80.leveldb.impl")
    automaticModule("org.iq80.leveldb:leveldb-api", "org.iq80.leveldb.api")
    automaticModule("net.daporkchop.lib:natives", "net.daporkchop.lib.natives")
    automaticModule("org.iq80.snappy:snappy", "org.iq80.snappy")
    automaticModule("io.airlift:aircompressor", "io.airlift.aircompressor")
    automaticModule("com.github.stephenc.jcip:jcip-annotations", "com.github.stephenc.jcip.annotations")
    automaticModule("aopalliance:aopalliance", "aopalliance.aop")
    module("com.google.guava:failureaccess", "com.google.guava.failureaccess") {
        patchRealModule()
    }
    automaticModule("com.google.code.findbugs:jsr305", "com.google.code.findbugs.jsr305")
    module("com.google.j2objc:j2objc-annotations", "com.google.j2objc.annotations") {
        patchRealModule()
    }
    automaticModule("com.google.guava:listenablefuture", "com.google.guava.listenablefuture")
    automaticModule("net.jodah:expiringmap", "net.jodah.expiringmap")
    automaticModule("org.osgi:org.osgi.resource", "org.osgi.resource")
    automaticModule("org.osgi:org.osgi.service.serviceloader", "org.osgi.service.serviceloader")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "cloudburst-server"
            from(components["java"])
            artifact(tasks.generateGitProperties) {
                extension = "properties"
            }
        }
    }
}

tasks.jar {
    archiveClassifier.set("dev")
}

gitProperties {
    failOnNoGitDirectory = false
    customProperty("github.repo", "CloudburstMC/Cloudburst")
}

tasks.shadowJar {
    archiveBaseName.set("Cloudburst")
    archiveVersion.set("")
    archiveClassifier.set("")

    duplicatesStrategy = DuplicatesStrategy.FAIL
    failOnDuplicateEntries = true

    // Resource mergers need every input before producing a single entry.
    filesMatching(
        listOf(
            "META-INF/services/**",
            "META-INF/org/apache/logging/log4j/core/config/plugins/Log4j2Plugins.dat",
            "META-INF/io.netty.versions.properties",
            "META-INF/LICENSE.md",
            "META-INF/proguard/gson.pro"
        )
    ) {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    manifest {
        attributes["Main-Class"] = "org.cloudburstmc.server.Bootstrap"
        attributes["Enable-Native-Access"] = "ALL-UNNAMED"
    }
    transform(Log4j2PluginsCacheFileTransformer())
    mergeServiceFiles()
    append("META-INF/io.netty.versions.properties")
    append("META-INF/LICENSE.md")
    append("META-INF/proguard/gson.pro")
    exclude(
        "META-INF/maven/com.google.code.gson/gson/**",
        "META-INF/versions/**/OSGI-INF/MANIFEST.MF",
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/NOTICE"
    )
}

tasks.register<JavaExec>("run") {
    mainClass.set("org.cloudburstmc.server.Bootstrap")
    workingDir = projectDir.resolve("run")
    workingDir.mkdir()
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true")
    systemProperty("guice_bytecode_gen_option", "DISABLED")
}
