import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
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

dependencies {
    api(project(":ksef"))
    api(project(":invoice"))
    implementation(libs.kotlinx.serialization.json)
}

testing {
    suites {
        val junitVersion = libs.versions.junit.get()

        named<JvmTestSuite>("test") {
            useJUnitJupiter(junitVersion)
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // Full synchronization against the real KSeF TEST environment. Not part of `check`/`build`.
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter(junitVersion)
            dependencies {
                implementation(project())
                implementation(testFixtures(project(":ksef")))
                implementation(libs.kotlinx.coroutines.test)
            }
            targets.all {
                testTask.configure {
                    description = "Synchronizes invoices from KSeF TEST (requires secrets/ksef-test.properties)."
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    systemProperty(
                        "ksef.secretsFile",
                        rootProject.layout.projectDirectory.file("secrets/ksef-test.properties").asFile.absolutePath,
                    )
                    outputs.upToDateWhen { false }
                    outputs.cacheIf { false }
                    shouldRunAfter(tasks.named("test"))
                }
            }
        }
    }
}
