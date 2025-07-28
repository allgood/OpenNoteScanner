plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    compileSdk = 36
    namespace = "com.todobom.opennotescanner"
    
    buildFeatures {
        buildConfig = true
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    defaultConfig {
        applicationId = "com.todobom.opennotescanner"
        minSdk = 21
        targetSdk = 36
        versionCode = 37
        versionNameSuffix = "alpha"
        versionName = "1.0.37"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20")

    implementation("com.guolindev.permissionx:permissionx:1.8.1")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.fragment:fragment-ktx:1.8.8")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.opencv:opencv:4.12.0")
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    implementation("com.github.ctodobom:drag-select-recyclerview:0.3.4.ctodobom.sections")
    implementation("com.github.allgood:Android-Universal-Image-Loader:717a00c")
    implementation("com.github.ctodobom:FabToolbar:3c5f0e0ff1b6d5089e20b7da7157a604075ae943")
    implementation("com.github.matomo-org:matomo-sdk-android:4.3.4")
    implementation("com.github.MikeOrtiz:TouchImageView:3.7.1")

    val itextpdfVersion = "9.2.0"
    implementation("com.itextpdf:kernel:$itextpdfVersion")
    implementation("com.itextpdf:layout:$itextpdfVersion")
    implementation("com.itextpdf:io:$itextpdfVersion")
}
