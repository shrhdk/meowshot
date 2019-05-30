/*
 * Copyright (C) 2018 Hideki Shiro
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

import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
}

val props = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    aaptOptions {
        noCompress("html")
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("meowshot.keystore")
            storePassword = props["android.keystore.password"] as String
            keyAlias = props["android.keystore.alias"] as String
            keyPassword = props["android.keystore.private_key_password"] as String
        }
    }
    buildTypes["release"].signingConfig = signingConfigs["release"]
}

dependencies {
    implementation(kotlin("stdlib-jdk8", KotlinCompilerVersion.VERSION))
    implementation("org.theta4j", "theta-plugin-sdk", "0.0.4")
    implementation("org.theta4j", "theta-web-api", "1.2.2")
    implementation("org.nanohttpd", "nanohttpd", "2.3.1")
}
