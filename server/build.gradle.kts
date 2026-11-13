import com.github.jengelman.gradle.plugins.shadow.transformers.Log4j2PluginsCacheFileTransformer
import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import java.nio.file.Files

plugins {
    alias(libs.plugins.git.properties)
    alias(libs.plugins.shadow)
}

java {
    modularity.inferModulePath.set(false)
}

repositories {
    maven("https://repo.lucko.me/") {
        content {
            includeGroup("me.lucko")
            includeModule("net.kyori", "adventure-text-feature-pagination")
        }
    }
}

@CacheableRule
public abstract class LibDataChannelAlignmentRule : ComponentMetadataRule {
    public override fun execute(context: ComponentMetadataContext) {
        context.details.run {
            belongsTo("dev.opencollab:libdatachannel-platform:${id.version}")
        }
    }
}

dependencies {
    components {
        withModule<LibDataChannelAlignmentRule>("dev.opencollab:libdatachannel-java")
        withModule<LibDataChannelAlignmentRule>("dev.opencollab:libdatachannel-java-arch-detect")
    }

    implementation(project(":api"))
    implementation(libs.snakeyaml)
    implementation(libs.bedrock.codec)
    api(libs.bedrock.connection) {
        exclude("com.nukkitx.fastutil")
    }
    api(libs.bedrock.adventure)
    implementation(libs.netty.transport.nethernet)
    implementation(libs.netty.external.signaling)
    runtimeOnly(libs.libdatachannel.natives) {
        exclude(group = "dev.opencollab", module = "libdatachannel-java")
    }
    implementation(libs.adventure.text.logger.slf4j)
    implementation(libs.adventure.text.serializer.plain)
    api(libs.block.state.updater)
    api(libs.bundles.fastutil)
    api(libs.leveldb.mcpe.jni)
    api(libs.noise)

    compileOnly(libs.jsr305)

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
    implementation(libs.spark.common) {
        exclude(group = "me.lucko", module = "spark-api")
    }
    implementation(libs.spark.api)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
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

tasks.test {
    // Generated-terrain measurements are opt-in.
    System.getProperty("cloudburst.lightBenchmark")?.let { systemProperty("cloudburst.lightBenchmark", it) }
}

tasks.register<JavaExec>("run") {
    val runDirectory = layout.projectDirectory.dir("run").asFile

    mainClass.set("org.cloudburstmc.server.Bootstrap")
    workingDir = runDirectory
    classpath = sourceSets["main"].runtimeClasspath
    standardInput = System.`in`
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    systemProperty("org.jline.terminal.disableDeprecatedProviderWarning", "true")
    systemProperty("guice_bytecode_gen_option", "DISABLED")

    doFirst {
        Files.createDirectories(runDirectory.toPath())
    }
}
