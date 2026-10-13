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
            }
        }

        // Calls the real White List API (wl-api.mf.gov.pl) with fictitious NIPs only. Not part of `check`/`build`.
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter(junitVersion)
            dependencies {
                implementation(project())
            }
            targets.all {
                testTask.configure {
                    description = "Runs tests against the real MF White List API (fictitious NIPs only)."
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    // Results depend on an external service, so never treat them as up to date or cached.
                    outputs.upToDateWhen { false }
                    outputs.cacheIf { false }
                    shouldRunAfter(tasks.named("test"))
                }
            }
        }
    }
}
