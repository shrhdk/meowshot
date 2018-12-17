/*
 * Copyright 2018 Hideki Shiro
 */

import org.jetbrains.kotlin.config.KotlinCompilerVersion;

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(28)
    defaultConfig {
        applicationId = "be.shiro.meowshot"
        minSdkVersion(25)
        targetSdkVersion(28)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }
    aaptOptions {
        noCompress("html")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation("com.android.support.constraint", "constraint-layout", "1.1.3")
    implementation("org.theta4j", "theta-plugin-sdk", "0.0.2")
    implementation("org.nanohttpd", "nanohttpd", "2.2.0")

    testImplementation("junit", "junit", "4.12")
    androidTestImplementation("com.android.support.test", "runner", "1.0.2")
    androidTestImplementation("com.android.support.test.espresso", "espresso-core", "3.0.2")
}
