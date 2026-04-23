import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.aboutlibraries)
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingProp(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotEmpty() }
        ?: keystoreProperties.getProperty(name)?.takeIf { it.isNotEmpty() }

val releaseStoreFile = signingProp("KEYSTORE_PATH")
    ?.let { rootProject.file(it) }
    ?: rootProject.file("release.keystore").takeIf { it.exists() }
val releaseStorePassword = signingProp("KEYSTORE_PASSWORD")
val releaseKeyAlias = signingProp("KEY_ALIAS")
val releaseKeyPassword = signingProp("KEY_PASSWORD")

val hasReleaseSigning = releaseStoreFile != null &&
    releaseStorePassword != null &&
    releaseKeyAlias != null &&
    releaseKeyPassword != null

android {
    namespace = "com.github.chudoxl.linteh.food"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.github.chudoxl.linteh.food"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
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
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                logger.lifecycle(
                    "[signing] Release signing config skipped — missing credentials. " +
                        "APK/AAB will be unsigned. Set KEYSTORE_PATH/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD " +
                        "via env vars (e.g. through scripts/release.sh) or keystore.properties."
                )
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE.md",
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
            )
        }
    }
}

base {
    archivesName.set("linteh-food-${android.defaultConfig.versionName}(${android.defaultConfig.versionCode})")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.datetime.wheel.picker)
    implementation(libs.kotlinx.datetime)
    implementation(libs.aboutlibraries.core)
    implementation(libs.aboutlibraries.compose.m3)
    implementation(libs.markdown.renderer.m3)

    testImplementation(libs.junit)
    testImplementation(libs.json.org)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.truth)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.mockk.android) {
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
        exclude(group = "org.junit", module = "junit-bom")
    }
    androidTestImplementation(libs.truth)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

abstract class GenerateReadmeAsset : DefaultTask() {
    @get:InputFile
    abstract val source: RegularFileProperty

    @get:OutputDirectory
    abstract val destination: DirectoryProperty

    @TaskAction
    fun run() {
        val outDir = destination.get().asFile
        outDir.mkdirs()
        source.get().asFile.copyTo(outDir.resolve("readme.md"), overwrite = true)
    }
}

val generateReadmeAsset by tasks.registering(GenerateReadmeAsset::class) {
    source.set(rootProject.file("README.md"))
    destination.set(layout.buildDirectory.dir("generated/readme-assets"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            generateReadmeAsset,
            GenerateReadmeAsset::destination,
        )
    }
}
