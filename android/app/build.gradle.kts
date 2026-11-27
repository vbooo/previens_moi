import java.util.Properties
import java.util.Base64
import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Only public mobile-client settings live here; server credentials never enter Gradle.
// Explicit -P properties still take precedence for isolated local and CI builds.
val clientProperties = Properties().apply {
    val file = rootProject.file(if (providers.gradleProperty("playApplicationId").isPresent) "play-client.properties" else "client.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val localServiceBuild = providers.gradleProperty("localService").getOrElse("false") == "true"
// Opt-in upload signing is local only. CI and ordinary release builds stay unsigned.
val playSigning = providers.gradleProperty("playSigning").getOrElse("false") == "true"
val uploadSigning = Properties().apply {
    if (playSigning) {
        check(providers.gradleProperty("playApplicationId").orNull == "fr.previensmoi.app" && !localServiceBuild) {
            "Upload signing requires the definitive Play package and remote service."
        }
        val settings = rootProject.file("../.local/play-signing/signing.properties")
        check(settings.isFile) { "Local upload signing settings missing; see docs/14-abonnement-google-play.md." }
        settings.inputStream().use { load(it) }
        check(listOf("storeFile", "storePassword", "keyAlias", "keyPassword").all { !getProperty(it).isNullOrBlank() }) {
            "Incomplete local upload signing settings."
        }
        check(rootProject.file(getProperty("storeFile")).isFile) { "Local upload keystore missing." }
    }
}
fun clientSetting(name: String, fallback: String = ""): String =
    providers.gradleProperty(name).getOrElse(
        if (localServiceBuild) fallback else clientProperties.getProperty(name, fallback),
    )


android {
    namespace = "fr.previensmoi"
    compileSdk = 37
    defaultConfig {
        applicationId = "fr.previensmoi.prototype"
        // Required by Google Play automatic protection.
        minSdk = 24
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        versionCode = 23
        versionName = "0.2.9"
    }
    if (playSigning) {
        signingConfigs.create("playUpload") {
            storeFile = rootProject.file(uploadSigning.getProperty("storeFile"))
            storePassword = uploadSigning.getProperty("storePassword")
            keyAlias = uploadSigning.getProperty("keyAlias")
            keyPassword = uploadSigning.getProperty("keyPassword")
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            if (playSigning) signingConfig = signingConfigs.getByName("playUpload")
        }
    }
    flavorDimensions += "environment"
    productFlavors {
        create("demo") { dimension = "environment"; applicationIdSuffix = ".demo" }
        create("remote") {
            isDefault = true
            val playId = providers.gradleProperty("playApplicationId").orNull
            if (playId != null) {
                check(playId == "fr.previensmoi.app") { "Unexpected Play application ID." }
                applicationId = playId
            }
            buildConfigField("String", "PLAY_PACKAGE_NAME", "\"fr.previensmoi.app\"")
            dimension = "environment"; applicationIdSuffix = if (playId == null) ".remote" else ""
            for ((field, property) in listOf("FIREBASE_APP_ID" to "firebaseAppId", "FIREBASE_PROJECT_ID" to "firebaseProjectId", "FIREBASE_API_KEY" to "firebaseApiKey", "FIREBASE_SENDER_ID" to "firebaseSenderId")) {
                buildConfigField("String", field, "\"${clientSetting(property)}\"")
            }
            buildConfigField("String", "SERVICE_URL", "\"${clientSetting("serviceUrl", "https://service.previens-moi.invalid/functions/v1/")}\"")
            buildConfigField("String", "AUTH_URL", "\"${clientSetting("authUrl", "https://service.previens-moi.invalid/auth/v1/")}\"")
            buildConfigField("String", "AUTH_PUBLIC_KEY", "\"${clientSetting("authPublicKey")}\"")
            buildConfigField("boolean", "LOCAL_SERVICE", providers.gradleProperty("localService").getOrElse("false"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets.getByName("test").resources.directories.add("../../contracts")
    lint {
        abortOnError = true
        warningsAsErrors = true
        // Pinned, compatible versions are deliberate. Updates remain visible in the report,
        // but a new upstream release must not break an otherwise unchanged build.
        informational += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion")
    }
}
// Only the connected app has a release variant. CI uses explicit public fixtures;
// a successful build does not itself authorize signing or publication.
androidComponents {
    beforeVariants(selector().withBuildType("release")) {
        if (it.productFlavors.any { (_, flavor) -> flavor == "demo" }) it.enable = false
    }
}
val validateRemoteReleaseConfiguration = tasks.register("validateRemoteReleaseConfiguration") {
    group = "verification"
    description = "Reject local/test-mode or incomplete connected release settings without logging values."
    doLast {
        if (providers.gradleProperty("playApplicationId").isPresent) {
            check(clientSetting("firebasePackageName") == "fr.previensmoi.app") { "Play release requires matching public Firebase package settings." }
        }
        check(!localServiceBuild) { "Release requires localService=false and explicit HTTPS client settings." }
        for ((name, expectedPath) in listOf("serviceUrl" to "/functions/v1/", "authUrl" to "/auth/v1/")) {
            val url = runCatching { URI(clientSetting(name)) }.getOrNull()
            check(url != null && url.scheme == "https" && url.host != null &&
                url.host !in setOf("localhost", "127.0.0.1", "10.0.2.2", "::1") &&
                !url.host.endsWith(".invalid") && url.rawUserInfo == null &&
                url.rawQuery == null && url.rawFragment == null && url.path == expectedPath) {
                "Release requires a valid HTTPS $name without credentials, query or fragment."
            }
        }
        val key = clientSetting("authPublicKey")
        val anonJwt = runCatching {
            val payload = String(Base64.getUrlDecoder().decode(key.split('.')[1]))
            Regex("\"role\"\\s*:\"anon\"").containsMatchIn(payload.replace(" ", ""))
        }.getOrDefault(false)
        check(anonJwt || key.startsWith("sb_publishable_")) { "Release requires an anonymous/public Supabase client key." }
        check(Regex("1:[0-9]+:android:[a-f0-9]+").matches(clientSetting("firebaseAppId")) &&
            Regex("[a-z][a-z0-9-]{4,62}").matches(clientSetting("firebaseProjectId")) &&
            Regex("[A-Za-z0-9_-]+").matches(clientSetting("firebaseApiKey")) &&
            Regex("[0-9]+").matches(clientSetting("firebaseSenderId"))) {
            "Release requires all public Firebase Android settings."
        }
    }
}
tasks.matching { it.name == "preRemoteReleaseBuild" }.configureEach {
    dependsOn(validateRemoteReleaseConfiguration)
}
dependencies {
    "remoteImplementation"(libs.play.billing)
    "remoteImplementation"(libs.firebase.messaging)
    "remoteImplementation"(libs.work.runtime)
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
    implementation(libs.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.serialization)
    implementation(libs.okhttp)
    implementation(libs.coroutines)
    implementation(libs.datastore)
    implementation(libs.hilt)
    ksp(libs.hilt.compiler)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.android.test.runner)
    androidTestImplementation(libs.android.test.junit)
    androidTestImplementation(libs.mockwebserver)
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
}
