import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.extra.java.module.info)
}

dependencies {
    api(projects.api)
    api(libs.bedrock.connection) {
        exclude("com.nukkitx.fastutil")
    }
    api(libs.block.state.updater)
    api(libs.bundles.fastutil)
    api(libs.leveldb.mcpe.jni)
    api(libs.noise)

    implementation(libs.terminal.console.appender)
    implementation(libs.jline.terminal)
    implementation(libs.jline.reader)
    runtimeOnly(libs.jline.terminal.jna)

    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
    runtimeOnly(libs.log4j.slf4j2.impl)
    runtimeOnly(libs.disruptor)
    implementation(libs.jopt.simple)
    implementation(libs.jose.jwt)
    implementation(libs.upnp)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.engine)
}

extraJavaModuleInfo {
    automaticModule(libs.block.state.updater, "org.cloudburstmc.blockstateupdater")
    automaticModule(libs.math.immutable, "org.cloudburstmc.math.immutable")
    automaticModule(libs.noise, "net.daporkchop.lib.noise")
    automaticModule(libs.upnp, "org.cloudburstmc.upnp")
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
    automaticModule("com.google.guava:failureaccess", "com.google.guava.failureaccess")
    automaticModule("com.google.code.findbugs:jsr305", "com.google.code.findbugs.jsr305")
    automaticModule("com.google.j2objc:j2objc-annotations", "com.google.j2objc.annotations")
    automaticModule("net.jodah:expiringmap", "net.jodah.expiringmap")
}


tasks.shadowJar {
    archiveBaseName.set("Cloudburst")
    archiveVersion.set("")
    archiveClassifier.set("")

    manifest {
        attributes["Main-Class"] = "org.cloudburstmc.server.Bootstrap"
    }
    transform(Log4j2PluginsCacheFileTransformer())
    mergeServiceFiles()
}