plugins {
    alias(libs.plugins.checkerframework) apply false
}

subprojects {
    apply(plugin = "java-library")
    if (name != "codegen") {
        apply(plugin = "maven-publish")
        apply(plugin = rootProject.libs.plugins.checkerframework.get().pluginId)

        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "opencollab"
                    url = uri("https://repo.opencollab.dev/maven-snapshots")
                    credentials {
                        username = System.getenv("DEPLOY_USERNAME")
                        password = System.getenv("DEPLOY_PASSWORD")
                    }
                }
            }
        }
    }

    group = "org.cloudburstmc"
    version = providers.gradleProperty("$name.version").get()

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    repositories {
        mavenCentral()
        maven("https://repo.opencollab.dev/maven-releases")
        maven("https://repo.opencollab.dev/maven-snapshots")
    }

    dependencies {
        "compileOnly"(rootProject.libs.lombok)
        "annotationProcessor"(rootProject.libs.lombok)

        "compileOnly"(rootProject.libs.checker.qual)

        "testAnnotationProcessor"(rootProject.libs.lombok)
        "testImplementation"(rootProject.libs.lombok)
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-implicit:none", "-Xlint:deprecation", "-Xlint:unchecked"))
        }

        withType<Test> {
            useJUnitPlatform()
            jvmArgs("--enable-native-access=ALL-UNNAMED")
        }
    }
}
