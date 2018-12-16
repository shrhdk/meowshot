/*
 * Copyright 2018 Hideki Shiro
 */

buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build", "gradle", "3.2.1")
        classpath(kotlin("gradle-plugin", "1.3.10"))
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}
