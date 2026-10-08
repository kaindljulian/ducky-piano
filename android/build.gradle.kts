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
        from(zipTree(classpath.map { it.singleFile })) {
            into(abi)
            include("*.so")
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
            java.setSrcDirs(listOf("src/main/java"))
            aidl.setSrcDirs(listOf("src/main/java"))
            renderscript.setSrcDirs(listOf("src/main/java"))
            res.setSrcDirs(listOf("res"))
            assets.setSrcDirs(listOf("../assets"))
            jniLibs.setSrcDirs(listOf(nativeLibsDirectory.get().asFile))
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
        applicationId = "com.github.kaindljulian.piano"
        minSdk = 21
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
    buildTypes {
        named("release") {
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
    }
}

tasks.register<Exec>("run") {
    description = "Starts the installed app on the connected device."
    commandLine(
        android.sdkDirectory.resolve("platform-tools/adb").absolutePath,
        "shell",
        "am",
        "start",
        "-n",
        "com.github.kaindljulian.piano/com.github.kaindljulian.piano.android.AndroidLauncher"
    )
}
