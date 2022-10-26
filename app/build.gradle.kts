plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 32
    buildToolsVersion = "32.0.0"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }
    defaultConfig {
        applicationId = "com.todobom.opennotescanner"
        minSdk = 21
        targetSdk = 31
        versionCode = 36
        versionName = "1.0.36"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }
    flavorDimensions += "version"
    productFlavors {
        create("fdroid") {
            dimension = "version"
            applicationIdSuffix = ""
            versionNameSuffix = "-fdroid"
        }
        create("gplay") {
            dimension = "version"
            applicationIdSuffix = ""
            versionNameSuffix = "-gplay"
        }
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(fileTree(mapOf("include" to listOf("*.jar"), "dir" to "libs")))

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.20")

    implementation("androidx.exifinterface:exifinterface:1.3.3")
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.appcompat:appcompat:1.4.2")
    implementation("com.google.android.material:material:1.6.1")
    implementation("com.google.zxing:core:3.5.0")
    implementation("com.github.ctodobom:OpenCV-3.1.0-Android:9e00ee4218ca0c9e60a905c9f09bf499f9dc5115")
    implementation("us.feras.mdv:markdownview:1.1.0")
    implementation("com.github.ctodobom:drag-select-recyclerview:0.3.4.ctodobom.sections")
    implementation("com.github.allgood:Android-Universal-Image-Loader:717a00c")
    implementation("com.github.ctodobom:FabToolbar:3c5f0e0ff1b6d5089e20b7da7157a604075ae943")
    implementation("com.github.matomo-org:matomo-sdk-android:4.1.4")
    implementation("com.github.MikeOrtiz:TouchImageView:3.2.0")

    val itextpdf_version = "7.2.4"
    implementation("com.itextpdf:kernel:$itextpdf_version")
    implementation("com.itextpdf:layout:$itextpdf_version")
    implementation("com.itextpdf:io:$itextpdf_version")
}
