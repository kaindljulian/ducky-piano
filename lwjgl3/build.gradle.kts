plugins {
    id("java-library")
    id("piano.conventions")
    id("asset-gen")
    application
}

val appName = providers.gradleProperty("appName").get()
val projectVersion = providers.gradleProperty("projectVersion").get()


sourceSets["main"].resources.srcDirs(rootProject.file("assets").path)

application {
    mainClass = "com.github.kaindljulian.piano.lwjgl3.Lwjgl3Launcher"
    applicationName = appName
}

dependencies {
    implementation(libs.gdxBackendLwjgl3)
    implementation(libs.gdxPlatform)
    implementation(variantOf(libs.gdxPlatform) { classifier("natives-desktop") })
    implementation(project(":core"))
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.jar {
    archiveFileName.set("$appName-$projectVersion.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    dependsOn(configurations.runtimeClasspath)
    from(configurations.runtimeClasspath.map { classpath ->
        classpath.map { if (it.isDirectory) it else zipTree(it) }
    })

    exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/maven/**")

    manifest {
        attributes(
            "Main-Class" to application.mainClass.get(),
            "Enable-Native-Access" to "ALL-UNNAMED",
            "Multi-Release" to "true"
        )
    }

    doLast {
        archiveFile.get().asFile.setExecutable(true, false)
    }
}

tasks.register("jarLinux") {
    description = "Builds a JAR that only includes the files needed to run on Linux"
    dependsOn("jar")
    group = "build"
    val jar = tasks.getByName<Jar>("jar")
    jar.archiveFileName.set("$appName-$projectVersion-linux.jar")
    jar.exclude(
        "windows/x86/**", "windows/x64/**", "macos/arm64/**", "macos/x64/**", "**/*.dll", "**/*.dylib",
        "META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/maven/**"
    )
}

tasks.register("jarWin") {
    description = "Builds a JAR that only includes the files needed to run on Windows"
    dependsOn("jar")
    group = "build"
    val jar = tasks.getByName<Jar>("jar")
    jar.archiveFileName.set("$appName-$projectVersion-win.jar")
    jar.exclude(
        "macos/arm64/**", "macos/x64/**", "linux/arm32/**", "linux/arm64/**", "linux/x64/**", "**/*.dylib", "**/*.so",
        "META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/maven/**"
    )
}

distributions {
    named("main") {
        contents {
            into("libs") {
                val jarName = tasks.jar.get().outputs.files.singleFile.name
                configurations.runtimeClasspath.get().files.filter { it.name != jarName }.forEach { file ->
                    exclude(file.name)
                }
            }
        }
    }
}
