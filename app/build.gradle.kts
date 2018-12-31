/*
 * Copyright (C) 2018 theta4j project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
        versionName = "1.0.0"
        testInstrumentationRunner = "android.support.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        setSourceCompatibility(JavaVersion.VERSION_1_8)
        setTargetCompatibility(JavaVersion.VERSION_1_8)
    }
    aaptOptions {
        noCompress("html")
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("meowshot.keystore")
            storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_KEYSTORE_ALIAS")
            keyPassword = System.getenv("ANDROID_KEYSTORE_PRIVATE_KEY_PASSWORD")
        }
    }
    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation("org.theta4j", "theta-plugin-sdk", "0.0.4")
    implementation("org.nanohttpd", "nanohttpd", "2.2.0")

    testImplementation("junit", "junit", "4.12")
    androidTestImplementation("com.android.support.test", "runner", "1.0.2")
    androidTestImplementation("com.android.support.test.espresso", "espresso-core", "3.0.2")
}
