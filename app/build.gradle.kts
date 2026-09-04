plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Applied only when the file is actually there. The plugin turns
// google-services.json into the string resources Firebase reads, and it fails
// the build when the file is missing — which would make the repository
// unbuildable for anyone who has not been given one (D002, D033).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.travelcompanion.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.travelcompanion.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Compose layout assertions need Android resources on the JVM
            // classpath. This is what lets the one geometry guard run in
            // `testDebugUnitTest` with everything else, rather than needing a
            // device (D056).
            isIncludeAndroidResources = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.activity.compose)
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)

    // Room arrives here and not before. Phase 0 has carried "add Room when
    // structured runtime persistence is first needed" since the beginning;
    // until now every runtime fact fitted in DataStore or came from the
    // packaged trip. Voice memories are the first thing with rows to query —
    // a list of previous recordings, by date, with an author (D057).
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Encoding only, and only for a package carrying real ticket data. Brief
    // §18 forbids a placeholder QR, so the generator is gated rather than
    // trusted (D061). The pure-Java `core` artefact, not the Android one:
    // nothing here needs a camera, a view or a permission.
    implementation(libs.zxing.core)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Group sync (Phase 4). The SDK is on the classpath unconditionally; what
    // is conditional is the configuration, so a build with no
    // google-services.json compiles, runs and simply never connects.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.database)
    // Anonymous sign-in only: it exists so database rules can require
    // auth != null instead of being open to anyone with the URL. There is no
    // account, no login screen and nothing for the traveller to do (D035).
    implementation(libs.firebase.auth)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui.compose)
    implementation(libs.google.play.services.location)

    testImplementation(libs.junit)
    // One guard, not a UI-test suite: `TcHero`'s backdrop covering the hero is
    // a layout fact no state assertion can see, and it is the defect this
    // repository actually shipped (D056).
    testImplementation(libs.robolectric)
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test:core")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
