plugins {
    id("piano.java-conventions")
    application
}

val appName = providers.gradleProperty("appName").get()
val projectVersion = providers.gradleProperty("projectVersion").get()

sourceSets["main"].resources.srcDir(rootProject.file("assets"))

application {
    mainClass = "com.github.kaindljulian.piano.lwjgl3.Lwjgl3Launcher"
    applicationName = appName
}

dependencies {
    implementation(project(":core"))
    implementation(libs.gdxBackendLwjgl3)
    runtimeOnly(variantOf(libs.gdxPlatform) { classifier("natives-desktop") })
    runtimeOnly(variantOf(libs.gdxFreetypePlatform) { classifier("natives-desktop") })
}

tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.jar {
    archiveFileName = "$appName-$projectVersion.jar"
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

distributions.named("main") {
    contents {
        val bundledJar = "$appName-$projectVersion.jar"
        // exclude dependendies they are already in the app jar above
        exclude { it.file.name.endsWith(".jar") && it.file.name != bundledJar }
    }
}
