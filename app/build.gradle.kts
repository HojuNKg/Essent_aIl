plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

android {
    namespace = "com.HojuNK.essent_all"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.HojuNK.essent_all"
        minSdk = 33
        targetSdk = 36
        versionCode = 13
        versionName = "0.6.3"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        jniLibs {
            useLegacyPackaging = false
            pickFirsts += listOf(
                "**/libjpeg.so",
                "**/libpngx.so",
                "**/libleptonica.so",
                "**/libtesseract.so"
            )
        }
    }
    applicationVariants.all {
    outputs.all {
        val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
        output.outputFileName = "EssentAI-${versionName}.apk"
    }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(libs.play.services.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.text.recognition.korean)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)
    implementation("com.github.adaptech-cz:tesseract4android:4.9.0") {
        exclude(group = "com.github.adaptech-cz.tesseract4android", module = "tesseract4android-openmp")
    }
    implementation(group = "", name = "glyph-matrix-sdk-2.0", ext = "aar")
    debugImplementation(libs.androidx.ui.tooling)
}
