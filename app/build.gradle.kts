plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.safebus.driver"
    compileSdk { version = release(37) }
    defaultConfig {
        applicationId = "com.safebus.driver"
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
    buildFeatures { compose = true }
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
}
