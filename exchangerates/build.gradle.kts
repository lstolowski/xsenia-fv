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
    api(project(":invoice"))
    api(libs.ktor.client.core)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
}

testing {
    suites {
        val junitVersion = libs.versions.junit.get()

        named<JvmTestSuite>("test") {
            useJUnitJupiter(junitVersion)
            dependencies {
                implementation(libs.ktor.client.mock)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // Calls the real NBP API (public, no secrets). Not part of `check`/`build`; run with ./gradlew integrationTest.
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter(junitVersion)
            dependencies {
                implementation(project())
            }
            targets.all {
                testTask.configure {
                    description = "Runs tests against the real NBP exchange rates API."
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    outputs.upToDateWhen { false }
                    outputs.cacheIf { false }
                    shouldRunAfter(tasks.named("test"))
                }
            }
        }
    }
}
