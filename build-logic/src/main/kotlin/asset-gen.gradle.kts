tasks.register("generateAssetList") {
    description = "generates assets.txt"
    inputs.dir("${project.rootDir}/assets/")

    val assetsFolder = File("${project.rootDir}/assets/")

    val assetsFile = File(assetsFolder, "assets.txt")
    assetsFile.delete()

    fileTree(assetsFolder).map { it.toRelativeString(assetsFolder) }.sorted().forEach {
        assetsFile.appendText(it + "\n")
    }
}

tasks.named("processResources") {
    dependsOn("generateAssetList")
}
