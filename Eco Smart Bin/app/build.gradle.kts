import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.smartgarbage"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.smartgarbage"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProperties = Properties()
        val localFile = rootProject.file("local.properties")
        if (localFile.exists()) {
            localProperties.load(FileInputStream(localFile))
        }

        val googleWebClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: ""
        val baseUrl = localProperties.getProperty("BASE_URL") ?: ""

        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${project.properties.getOrDefault("GOOGLE_WEB_CLIENT_ID", "")}\"")
            buildConfigField("String", "BASE_URL", "\"${project.properties.getOrDefault("BASE_URL", "")}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("androidx.activity:activity:1.9.2")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.transport.api)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("com.github.bumptech.glide:glide:4.16.0") //glide  pic
    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")//json
    //Geson converter
    implementation ("com.squareup.retrofit2:converter-gson:2.11.0")
    //swipe to refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    //
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    //ads
    //implementation("com.google.android.gms:play-services-ads:23.6.0")
    //firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    //authoritaion
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation ("com.google.android.gms:play-services-auth:21.1.0")
    //lottie
    implementation("com.airbnb.android:lottie:6.4.0")
    implementation("com.airbnb.android:lottie-compose:6.4.0")
    //taps
        implementation ("com.google.android.material:material:1.9.0")
        implementation ("androidx.viewpager2:viewpager2:1.0.0")
    //QRCode
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")
    //gps
    implementation("com.google.android.gms:play-services-location:21.2.0")
    // local
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
}