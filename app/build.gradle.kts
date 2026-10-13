import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.BuiltArtifactsLoader
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Properties
import javax.inject.Inject

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing key (ZAD-015): secrets/keystore.properties, kept out of git (see README.md).
// Another file can be given with -Pxsenia.signingProperties=<path relative to the project root>.
// Passwords may come from XSENIA_STORE_PASSWORD / XSENIA_KEY_PASSWORD instead of the file.
val releaseSigningPropertiesPath = providers.gradleProperty("xsenia.signingProperties").orElse("secrets/keystore.properties")
val signingProperties = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file(releaseSigningPropertiesPath)).asText.orNull?.let { load(it.reader()) }
}

fun signingValue(key: String, environmentVariable: String? = null): String? =
    environmentVariable?.let { providers.environmentVariable(it).orNull }?.takeIf { it.isNotBlank() }
        ?: signingProperties.getProperty(key)?.takeIf { it.isNotBlank() }

val releaseStoreFile = signingValue("storeFile")?.let { rootProject.file(it) }
val releaseStorePassword = signingValue("storePassword", "XSENIA_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias")
val releaseKeyPassword = signingValue("keyPassword", "XSENIA_KEY_PASSWORD") ?: releaseStorePassword
val releaseSigningMissing = listOfNotNull(
    "storeFile".takeIf { releaseStoreFile == null },
    "storePassword".takeIf { releaseStorePassword == null },
    "keyAlias".takeIf { releaseKeyAlias == null },
)

android {
    namespace = "pl.xsenia.fv"
    compileSdk = 37

    defaultConfig {
        applicationId = "pl.xsenia.fv"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        // Flavor names must not start with "test" (reserved by AGP for test source sets).
        create("ksefTest") {
            dimension = "environment"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            buildConfigField("String", "KSEF_ENVIRONMENT", "\"TEST\"")
        }
        create("ksefProd") {
            dimension = "environment"
            buildConfigField("String", "KSEF_ENVIRONMENT", "\"PROD\"")
        }
    }

    signingConfigs {
        create("release") {
            if (releaseSigningMissing.isEmpty()) {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Without a key the release APK is built unsigned; the releaseApk task then fails with instructions.
            if (releaseSigningMissing.isEmpty()) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // targetSdk 36 matches the only target device (Galaxy S23, Android 16) — see ADR 0002.
        disable += "OldTargetApi"
        warningsAsErrors = true
    }

    testOptions {
        unitTests.all { it.useJUnitPlatform() }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":ksef"))
    implementation(project(":invoice"))
    implementation(project(":mirror"))
    implementation(project(":whitelist"))
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.client.mock)
    testRuntimeOnly(libs.junit.platform.launcher)
}

/**
 * Copies the signed release APK of one variant to build/dist/, after checking its signature with apksigner,
 * and prints the signing certificate digest (every update must be signed with the same key).
 */
abstract class ReleaseApkTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:InputFiles
    abstract val apkDirectory: DirectoryProperty

    @get:Internal
    abstract val artifactsLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val missingSigningValues: ListProperty<String>

    @get:Input
    abstract val signingPropertiesPath: Property<String>

    @get:InputFile
    abstract val apksigner: RegularFileProperty

    @get:Input
    abstract val apkName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun copyApk() {
        val missing = missingSigningValues.get()
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Release signing key is not configured (missing: ${missing.joinToString()}). " +
                    "Create ${signingPropertiesPath.get()} as described in README.md.",
            )
        }
        val builtApk = artifactsLoader.get().load(apkDirectory.get())?.elements?.singleOrNull()?.outputFile
            ?: throw GradleException("Release APK not found in ${apkDirectory.get().asFile}")
        val target = outputDirectory.file(apkName.get()).get().asFile
        File(builtApk).copyTo(target, overwrite = true)

        val certificates = ByteArrayOutputStream()
        exec.exec {
            commandLine(apksigner.get().asFile.absolutePath, "verify", "--print-certs", target.absolutePath)
            standardOutput = certificates
        }
        val certificateDigest = certificates.toString().lineSequence().firstOrNull { "certificate SHA-256 digest" in it }?.substringAfter(": ")
        val apkDigest = MessageDigest.getInstance("SHA-256").digest(target.readBytes()).joinToString("") { "%02x".format(it) }
        logger.lifecycle("Signed APK: ${target.absolutePath}")
        logger.lifecycle("Signing certificate SHA-256: $certificateDigest")
        logger.lifecycle("APK SHA-256: $apkDigest")
    }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        // releaseApk — KSeF PROD (the app for the phone); releaseApkTest — KSeF TEST, for checking a release build.
        val taskName = if (variant.flavorName == "ksefProd") "releaseApk" else "releaseApkTest"
        val versionName = variant.outputs.single().versionName
        val buildToolsPath = "build-tools/${android.buildToolsVersion}"
        val buildToolsDirectory = sdkComponents.sdkDirectory.map { it.dir(buildToolsPath) }
        tasks.register<ReleaseApkTask>(taskName) {
            group = "distribution"
            description = "Builds the signed ${variant.name} APK and copies it to build/dist/ (see README.md)."
            apkDirectory.set(variant.artifacts.get(SingleArtifact.APK))
            artifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
            missingSigningValues.set(releaseSigningMissing)
            signingPropertiesPath.set(releaseSigningPropertiesPath)
            apksigner.set(buildToolsDirectory.map { it.file("apksigner") })
            apkName.set(versionName.map { if (variant.flavorName == "ksefProd") "xsenia-$it.apk" else "xsenia-test-$it.apk" })
            outputDirectory.set(rootProject.layout.buildDirectory.dir("dist"))
        }
    }
}
