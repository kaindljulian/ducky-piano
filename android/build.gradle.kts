@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
}

val nativeLibsDirectory = layout.buildDirectory.dir("nativeLibs")

val nativeAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")

val gdxNativesByAbi = nativeAbis.associateWith { abi ->
    val declared = configurations.dependencyScope("gdxNatives-$abi")
    configurations.resolvable("gdxNativesClasspath-$abi") {
        extendsFrom(declared.get())
    }
}

val extractNativeLibs = tasks.register<Sync>("extractNativeLibs") {
    description = "Extracts the libGDX native libraries packaged into the APK."
    into(nativeLibsDirectory)
    gdxNativesByAbi.forEach { (abi, classpath) ->
        classpath.get().forEach { jar ->
            from(zipTree(jar)) {
                into(abi)
                include("*.so")
            }
        }
    }
}

tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn(extractNativeLibs)
}

android {
    namespace = "com.github.kaindljulian.piano"
    compileSdk = 36
    sourceSets {
        named("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.directories.apply { clear(); add("src/main/java") }
            aidl.directories.apply { clear(); add("src/main/java") }
            renderscript.directories.apply { clear(); add("src/main/java") }
            res.directories.apply { clear(); add("res") }
            assets.directories.apply { clear(); add("../assets") }
            jniLibs.directories.apply { clear(); add(nativeLibsDirectory.get().asFile.path) }
        }
    }
    packaging {
        resources {
            excludes += listOf(
                "META-INF/robovm/ios/robovm.xml", "META-INF/DEPENDENCIES.txt", "META-INF/DEPENDENCIES",
                "META-INF/dependencies.txt", "**/*.gwt.xml"
            )
            pickFirsts += listOf(
                "META-INF/LICENSE.txt", "META-INF/LICENSE", "META-INF/license.txt", "META-INF/LGPL2.1",
                "META-INF/NOTICE.txt", "META-INF/NOTICE", "META-INF/notice.txt"
            )
            jniLibs {
                keepDebugSymbols += "**/*.so"
            }
        }
    }
    defaultConfig {
        applicationId = "com.kaindl.ducky"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        encoding = "UTF-8"
        isCoreLibraryDesugaringEnabled = true
    }
    val uploadStoreFile = providers.gradleProperty("duckyPianoUploadStoreFile").orNull
    if (uploadStoreFile != null) {
        signingConfigs {
            create("upload") {
                storeFile = file(uploadStoreFile)
                storePassword = providers.gradleProperty("duckyPianoUploadStorePassword").get()
                keyAlias = providers.gradleProperty("duckyPianoUploadKeyAlias").get()
                keyPassword = providers.gradleProperty("duckyPianoUploadKeyPassword").get()
            }
        }
    }
    buildTypes {
        named("release") {
            if (uploadStoreFile != null) {
                signingConfig = signingConfigs.getByName("upload")
            }
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugarJdkLibs)
    implementation(project(":core"))
    implementation(libs.gdxBackendAndroid)
    nativeAbis.forEach { abi ->
        addProvider("gdxNatives-$abi", variantOf(libs.gdxPlatform) { classifier("natives-$abi") })
        addProvider("gdxNatives-$abi", variantOf(libs.gdxFreetypePlatform) { classifier("natives-$abi") })
    }
}

val adb = androidComponents.sdkComponents.adb

tasks.register<Exec>("run") {
    description = "Starts the installed app on the connected device."
    commandLine(
        adb.get().asFile.absolutePath,
        "shell",
        "am",
        "start",
        "-n",
        "com.kaindl.ducky/com.github.kaindljulian.piano.android.AndroidLauncher"
    )
}
