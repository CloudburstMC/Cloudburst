import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import java.util.Properties

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.extra.java.module.info)
}

dependencies {
    implementation(project(":api")) {
        attributes {
            attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.CLASSES))
        }
    }
    api(libs.bedrock.connection) {
        exclude("com.nukkitx.fastutil")
    }
    api(libs.block.state.updater)
    api(libs.bundles.fastutil)
    api(libs.leveldb.mcpe.jni)
    api(libs.noise)

    compileOnly(libs.jsr305)

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
    failOnAutomaticModules.set(false)
    automaticModule(libs.block.state.updater, "org.cloudburstmc.blockstateupdater") {
        overrideModuleName()
    }
    automaticModule(libs.math.immutable, "org.cloudburstmc.math.immutable") {
        overrideModuleName()
    }
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

tasks.shadowJar {
    archiveBaseName.set("Cloudburst")
    archiveVersion.set("")
    archiveClassifier.set("")

    // Shadow 9.x defaults to DuplicatesStrategy.EXCLUDE which prevents
    // Log4j2PluginsCacheFileTransformer from merging all Log4j2Plugins.dat files.
    // INCLUDE is required so the transformer sees .dat files from every dependency.
    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    manifest {
        attributes["Main-Class"] = "org.cloudburstmc.server.Bootstrap"
    }
    transform(Log4j2PluginsCacheFileTransformer())
    mergeServiceFiles()

    dependsOn(":api:classes", ":api:jar")
    from(project(":api").sourceSets.main.get().output)
}

tasks.register<JavaExec>("run") {
    mainClass.set("org.cloudburstmc.server.Bootstrap")
    workingDir = projectDir.resolve("run")
    workingDir.mkdir()
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs(
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens", "java.base/java.lang=ALL-UNNAMED",
        "--add-opens", "java.base/sun.nio.ch=ALL-UNNAMED"
    )
    systemProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true")
    systemProperty("guice_bytecode_gen_option", "DISABLED")
    systemProperty("io.netty.noUnsafe", "true")
}

abstract class GenerateGitPropertiesTask : DefaultTask() {
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Inject
    abstract val providers: ProviderFactory

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        val properties = Properties()

        try {
            val gitCommit = providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.get().trim()
            properties["git.commit.id.abbrev"] = gitCommit

            val gitBranch = providers.exec {
                commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            }.standardOutput.asText.get().trim()
            properties["git.branch"] = gitBranch

            val gitCommitTime = providers.exec {
                commandLine("git", "log", "-1", "--format=%ct")
            }.standardOutput.asText.get().trim()
            properties["git.commit.time"] = gitCommitTime
        } catch (e: Exception) {
            println("Warning: Could not retrieve git information: ${e.message}")
            properties["git.commit.id.abbrev"] = "unknown"
        }

        file.outputStream().use { properties.store(it, "Git Information") }
    }
}

tasks.register<GenerateGitPropertiesTask>("generateGitProperties") {
    outputFile.set(layout.buildDirectory.file("resources/main/git.properties"))
    notCompatibleWithConfigurationCache("Executes git commands at runtime")
}

tasks.processResources {
    dependsOn("generateGitProperties")
}
