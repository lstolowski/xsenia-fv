import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-test-fixtures`
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
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    // KsefTestConfig — shared by the integration tests of all modules.
    testFixturesImplementation(platform(libs.junit.bom))
    testFixturesImplementation(libs.junit.jupiter)
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

        // Tests calling the real KSeF TEST environment. Not part of `check`/`build`; run explicitly.
        register<JvmTestSuite>("integrationTest") {
            useJUnitJupiter(junitVersion)
            dependencies {
                implementation(project())
                implementation(testFixtures(project()))
                implementation(project(":invoice"))
                implementation(libs.kotlinx.coroutines.test)
            }
            targets.all {
                testTask.configure {
                    description = "Runs tests against the KSeF TEST environment (requires secrets/ksef-test.properties)."
                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    systemProperty(
                        "ksef.secretsFile",
                        rootProject.layout.projectDirectory.file("secrets/ksef-test.properties").asFile.absolutePath,
                    )
                    // Tests creating invoices in KSeF TEST run only on request: ./gradlew integrationTest -PsendInvoices
                    systemProperty("ksef.sendInvoices", providers.gradleProperty("sendInvoices").isPresent.toString())
                    // Results depend on an external service, so never treat them as up to date or cached.
                    outputs.upToDateWhen { false }
                    outputs.cacheIf { false }
                    shouldRunAfter(tasks.named("test"))
                }
            }
        }
    }
}
