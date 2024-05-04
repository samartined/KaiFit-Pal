import java.util.Properties

plugins {
    id("com.android.application")
}

android {
    namespace = "com.tfg.kaifit_pal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tfg.kaifit_pal"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "AKy_URL", "\"${properties.getProperty("AKy")}\"")
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        buildConfig =
            true // This allows you to generate a BuildConfig class with constants that you can use in your app.

        viewBinding =
            true // Activates view binding in the module. This is a replacement for findViewById, and is used to bind views to data sources.

        dataBinding =
            true; // This allows you to bind UI components in your layouts to data sources in your app using a declarative format rather than programmatically.
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}