plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.solenstadabdatabase"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.solenstadabdatabase"
        minSdk = 24
        targetSdk = 35
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    dependencies {
        implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
        implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
        implementation("androidx.security:security-crypto:1.1.0-alpha06")
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        implementation("androidx.appcompat:appcompat:1.7.0")
        implementation("com.google.android.material:material:1.10.0")
        implementation("androidx.activity:activity:1.9.0")
        implementation("androidx.constraintlayout:constraintlayout:2.2.0")
        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    }
}