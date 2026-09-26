plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

android {
    namespace = "it.polimi.webeep"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.alethegoat.webeep"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("KEYSTORE_FILE")
            if (!storePath.isNullOrBlank()) {
                val keystoreFile = file(storePath)
                if (!keystoreFile.exists()) {
                    logger.warn("KEYSTORE_FILE not found: $storePath. Release signing will fail if requested.")
                } else {
                    storeFile = keystoreFile
                    storePassword = System.getenv("KEYSTORE_PASSWORD").orEmpty()
                    keyAlias = System.getenv("KEY_ALIAS").orEmpty()
                    keyPassword = System.getenv("KEY_PASSWORD").orEmpty()
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release packaging/signing tasks fail fast below when no keystore is
            // configured; the debug placeholder only keeps lint/unit tests working.
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile?.exists() == true) {
                releaseSigning
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    sourceSets["androidTest"].assets.srcDir("$projectDir/schemas")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

// Configuration-cache safe: the doFirst action captures only a Boolean.
val hasReleaseKeystore = System.getenv("KEYSTORE_FILE")
    ?.takeIf { it.isNotBlank() }
    ?.let { file(it).exists() } == true
val allowDebugReleaseSigning = System.getenv("ALLOW_DEBUG_RELEASE_SIGNING") == "true"
tasks.matching { it.name == "packageRelease" || it.name == "signReleaseBundle" }.configureEach {
    val keystoreMissing = !hasReleaseKeystore
    val debugFallbackAllowed = allowDebugReleaseSigning
    doFirst {
        if (keystoreMissing && !debugFallbackAllowed) {
            throw GradleException(
                "Missing release keystore: set KEYSTORE_FILE/KEYSTORE_PASSWORD/KEY_ALIAS/KEY_PASSWORD " +
                    "or set ALLOW_DEBUG_RELEASE_SIGNING=true for CI smoke builds."
            )
        }
        if (keystoreMissing) {
            logger.warn("Signing release with debug keystore (ALLOW_DEBUG_RELEASE_SIGNING=true, CI smoke only).")
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(rootProject.file("config/detekt/detekt.yml"))
    baseline = rootProject.file("config/detekt/baseline.xml")
    // Blocking: new findings fail the build; legacy complexity lives in the baseline.
    ignoreFailures = false
}

// Keep detekt on the Kotlin version it was compiled against.
configurations.matching { it.name == "detekt" }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlin") {
            useVersion(io.gitlab.arturbosch.detekt.getSupportedKotlinVersion())
        }
    }
}

ktlint {
    android.set(true)
    ignoreFailures.set(true)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.generateKotlin", "true")
}

composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_compiler")
    metricsDestination = layout.buildDirectory.dir("compose_compiler")
}


dependencies {
    // AndroidX Core
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Compose (BOM)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.text.google.fonts)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.hilt.navigation.compose)

    // Ktor & OkHttp
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore & Security
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
    implementation(libs.tink.android)

    // Coil (image loading)
    implementation(libs.coil.compose)

    // Browser (CustomTabs per SSO)
    implementation(libs.browser)

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)

    // Media3 (Video/Audio Player)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.datasource.okhttp)

    // WorkManager
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler.ext)

    // Debug
    debugImplementation(libs.compose.ui.tooling)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.ext.junit)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.coroutines.test)
    androidTestImplementation(libs.room.testing)
}
