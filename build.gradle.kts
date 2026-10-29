import org.gradle.language.jvm.tasks.ProcessResources

subprojects {
    apply(plugin = "java-library")
    if (name != "codegen") {
        apply(plugin = "maven-publish")

        configure<PublishingExtension> {
            publications.withType<MavenPublication>().configureEach {
                pom {
                    licenses {
                        license {
                            name.set("GNU Lesser General Public License, version 3 or later")
                            url.set("https://www.gnu.org/licenses/lgpl-3.0.html")
                            distribution.set("repo")
                        }
                    }
                }
            }

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
        maven("https://libraries.minecraft.net")
        maven("https://repo.opencollab.dev/maven-releases")
        maven("https://repo.opencollab.dev/maven-snapshots")
    }

    dependencies {
        "compileOnly"(rootProject.libs.lombok)
        "annotationProcessor"(rootProject.libs.lombok)

        "compileOnly"(rootProject.libs.checker.qual)
        "testCompileOnly"(rootProject.libs.checker.qual)

        "testAnnotationProcessor"(rootProject.libs.lombok)
        "testImplementation"(rootProject.libs.lombok)
    }

    tasks {
        named<ProcessResources>("processResources") {
            from(rootProject.layout.projectDirectory) {
                include("LICENSE", "LICENSE.LESSER")
                into("META-INF/cloudburst")
            }
        }

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
