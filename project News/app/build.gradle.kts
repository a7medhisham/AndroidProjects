plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.candroid2"
    compileSdk = 35

    buildFeatures{
        viewBinding= true
    }
    defaultConfig {
        applicationId = "com.example.candroid2"
        minSdk = 29
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    //glide .. pic
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //Retrofit...>بيجيب بيانات من سرفر
    implementation("com.squareup.retrofit2:retrofit:2.11.0")//json
    //Geson converter ...>after get Data retrofit
    implementation ("com.squareup.retrofit2:converter-gson:2.11.0")
    //swipe to refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    //
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    //ads
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    //firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    //authoritaion
    implementation("com.google.firebase:firebase-auth")

}