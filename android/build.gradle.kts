import java.util.Properties

plugins {
    id("piano.conventions")
    id("com.android.application")
}

val natives = configurations.create("natives")

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
            jniLibs.setSrcDirs(listOf("libs"))
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
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    implementation(libs.gdxBackendAndroid)
    implementation(project(":core"))
    natives(libs.gdxPlatform)
}

tasks.register("copyAndroidNatives") {
    description = "Extract natives into libs/ folders to package with APK"
    doFirst {
        file("libs/armeabi-v7a/").mkdirs()
        file("libs/arm64-v8a/").mkdirs()
        file("libs/x86_64/").mkdirs()
        file("libs/x86/").mkdirs()

        configurations.getByName("natives").copy().files.forEach { jar ->
            val outputDir: File? = when {
                jar.name.endsWith("natives-armeabi-v7a.jar") -> file("libs/armeabi-v7a")
                jar.name.endsWith("natives-arm64-v8a.jar") -> file("libs/arm64-v8a")
                jar.name.endsWith("natives-x86_64.jar") -> file("libs/x86_64")
                jar.name.endsWith("natives-x86.jar") -> file("libs/x86")
                else -> null
            }
            if (outputDir != null) {
                copy {
                    from(zipTree(jar))
                    into(outputDir)
                    include("*.so")
                }
            }
        }
    }
}

tasks.matching { it.name.contains("merge") && it.name.contains("JniLibFolders") }.configureEach {
    dependsOn("copyAndroidNatives")
}

tasks.register<Exec>("run") {
    val localProperties = project.file("../local.properties")

    val path = if (localProperties.exists()) {
        val properties = Properties()
        localProperties.inputStream().use { properties.load(it) }
        properties.getProperty("sdk.dir") ?: System.getenv("ANDROID_SDK_ROOT")
    } else {
        System.getenv("ANDROID_SDK_ROOT")
    }

    val adb = "$path/platform-tools/adb"
    commandLine(
        adb,
        "shell",
        "am",
        "start",
        "-n",
        "com.github.kaindljulian.piano/com.github.kaindljulian.piano.android.AndroidLauncher"
    )
}
