plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("maven-publish")
}

android {
    namespace = "com.link.core"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    explicitApi() // Upgraded to STRICT mode because we fixed everything!
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.link"
            artifactId = "core"
            version = "1.1.0" // Bumped version

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    // Hilt
    api(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Retrofit + OkHttp + kotlinx.serialization
    api(libs.retrofit)
    api(libs.retrofit.kotlinx.serialization)
    api(libs.okhttp)
    api(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Compose (for ThemeController/LocaleController)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)
}
