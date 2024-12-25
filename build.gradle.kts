plugins {
    id("parent-logic")
    id ("architectury-plugin") version "3.4-SNAPSHOT" apply false
    id ("dev.architectury.loom") version "1.7-SNAPSHOT" apply false
}

defaultTasks = arrayListOf("build", "publishToMavenLocal")

subprojects {
    apply(plugin = "base-conventions")
}

val universalJar = tasks.register<Jar>("universalJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    archiveFileName.set("OpenTerrainGenerator-Universal-" + project.property("otg_version").toString() + ".jar")
}

tasks.build {
    dependsOn(universalJar)
}

listOf(
    //project(":platforms:paper"),
    //project(":platforms:forge"),
    project(":platforms:fabric"),
).forEach { proj ->
    proj.afterEvaluate {
        // Show more errors in intellij
        proj.tasks.withType<JavaCompile>() {
            options.compilerArgs.add("-Xmaxerrs")
            options.compilerArgs.add("5000")
        }
        universalJar {
            val tree = zipTree(proj.the<OTGPlatformExtension>().productionJar)
            from(tree)
//            val manifestFile = tree.elements.map { files ->
//                files.find { it.asFile.path.endsWith("META-INF/MANIFEST.MF") }!!
//            }
//            manifest.from(manifestFile)
        }
    }
}
