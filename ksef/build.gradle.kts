import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

testing {
    suites {
        val junitVersion = libs.versions.junit.get()

        named<JvmTestSuite>("test") {
            useJUnitJupiter(junitVersion)
        }

        // Tests calling the real KSeF TEST environment. Not part of `check`/`build`; run explicitly.
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter(junitVersion)
            dependencies {
                implementation(project())
            }
            targets.all {
                testTask.configure {
                    description = "Runs tests against the KSeF TEST environment (requires secrets/ksef-test.properties)."
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    systemProperty(
                        "ksef.secretsFile",
                        rootProject.layout.projectDirectory.file("secrets/ksef-test.properties").asFile.absolutePath,
                    )
                    // Results depend on an external service, so never treat them as up to date or cached.
                    outputs.upToDateWhen { false }
                    outputs.cacheIf { false }
                    shouldRunAfter(tasks.named("test"))
                }
            }
        }
    }
}
