plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("maven-publish")
}

ksp {
    arg("dagger.hilt.disableModulesHaveInstallInCheck", "true")
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

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        buildConfig = true
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
            version = "1.3.0"

            afterEvaluate {
                from(components["release"])
            }

            // Force Gradle to declare this as an Android library in the POM file!
            pom {
                packaging = "aar"
            }
        }
    }
}

dependencies {
    // Hilt
    api(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Koin
    api(libs.koin.android)

    // Ktor client
    api(libs.ktor.client.core)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.json)
    api(libs.ktor.client.logging)

    // Retrofit + OkHttp + kotlinx.serialization
    api(libs.retrofit)
    api(libs.retrofit.kotlinx.serialization)
    api(libs.okhttp)
    api(libs.okhttp.logging)
    api(libs.kotlinx.serialization.json)

    // Firebase (optional): compileOnly so apps that don't use Firebase don't pull it in.
    // Apps using AuthService/FirestoreService must add firebase-auth / firebase-firestore themselves.
    compileOnly(platform(libs.firebase.bom))
    compileOnly(libs.firebase.auth)
    compileOnly(libs.firebase.firestore)

    // DataStore
    implementation(libs.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Compose (for ThemeController/LocaleController)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel)

    // Unit tests
    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
