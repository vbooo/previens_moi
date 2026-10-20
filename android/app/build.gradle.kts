plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
android {
    namespace = "fr.previensmoi"
    compileSdk = 37
    defaultConfig {
        applicationId = "fr.previensmoi.prototype"
        minSdk = 23
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 1
        versionName = "0.1.0"
    }
    flavorDimensions += "environment"
    productFlavors {
        create("demo") { dimension = "environment"; applicationIdSuffix = ".demo" }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    lint {
        abortOnError = true
        warningsAsErrors = true
        // Pinned, compatible versions are deliberate. Updates remain visible in the report,
        // but a new upstream release must not break an otherwise unchanged build.
        informational += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}
// No demo release artifact: publication is a separate, explicit sprint.
androidComponents { beforeVariants(selector().withBuildType("release")) { it.enable = false } }
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.runtime)
    implementation(libs.navigation.ui)
    implementation(libs.serialization)
    implementation(libs.coroutines)
    implementation(libs.datastore)
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
