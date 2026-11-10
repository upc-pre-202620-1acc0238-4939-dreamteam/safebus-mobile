plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.dreamteam.safebus"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.dreamteam.safebus"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true; buildConfig = true }
    buildTypes {
        val configured = providers.gradleProperty("safebusApiUrl")
        fun literal(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        debug { buildConfigField("String", "API_BASE_URL", literal(configured.orElse("http://10.0.2.2:8080").get())) }
        release { buildConfigField("String", "API_BASE_URL", literal(configured.orElse("").get())) }
    }
    // Keep both bundled languages available to the in-app language selector.
    bundle { language { enableSplit = false } }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.tooling)
    debugImplementation(libs.compose.test.manifest)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
    testImplementation(libs.androidx.test.junit)
}
