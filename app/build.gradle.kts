import io.gitlab.arturbosch.detekt.Detekt

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.detekt)
}

android {
    namespace = "fr.olegueyan.algomix"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "fr.olegueyan.algomix"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        jvmToolchain(21)
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        xmlReport = true
        htmlReport = true
    }

}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom("$rootDir/config/detekt/detekt.yml")
}

tasks.register<Detekt>("detektFormat") {
    description = "Automatically applies Detekt formatting rules to Kotlin sources."
    group = "formatting"
    parallel = true
    buildUponDefaultConfig = true
    autoCorrect = true
    disableDefaultRuleSets = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    setSource(files("src/main/java", "src/test/java"))
    include("**/*.kt", "**/*.kts")
    basePath = rootDir.absolutePath
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.supabase.bom))
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.ktor.client.android)
    detektPlugins(libs.detekt.formatting)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
}
