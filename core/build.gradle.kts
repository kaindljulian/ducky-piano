plugins {
    id("java-library")
    id("piano.java-conventions")
}

dependencies {
    api(libs.gdxCore)
    api(libs.gdxFreetype)
    testImplementation(libs.junitJupiter)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

tasks.test {
    useJUnitPlatform()
}
