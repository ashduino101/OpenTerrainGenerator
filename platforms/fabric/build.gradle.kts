plugins {
    id("platform-conventions")
    id("com.gradleup.shadow")
    id("architectury-plugin")
    id("dev.architectury.loom")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

//configurations {
//    common
//    shadowCommon // Don't use shadow from the shadow plugin since it *excludes* files.
//    compileClasspath.extendsFrom common
//    runtimeClasspath.extendsFrom common
//    developmentFabric.extendsFrom common
//}

val otg: Configuration by configurations.creating
configurations {
    implementation {
        extendsFrom(otg)
    }
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${project.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_api_version")}")
    // Remove the next line if you don't want to depend on the API
    //modApi "dev.architectury:architectury-fabric:${rootProject.architectury_version}"

    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())

    //common(project(path: ":platforms:shared", configuration: "namedElements")) { transitive false }
    //shadowCommon(project(path: ":platforms:shared", configuration: "transformProductionFabric")) { transitive false }

    otg(project(":common:common-core"))

    //implementation project(':platforms:shared')

    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")

    // shadowCommon project(':common:common-util')
    // shadowCommon project(':common:common-customobject')
    // shadowCommon project(':common:common-generator')
    // shadowCommon project(':common:common-core')

    //shadowCommon(project(':platforms:shared')) { transitive = false }
}

loom {
    //accessWidenerPath = file("src/main/resources/META-INF/otg.accesswidener")
}

tasks {
    processResources {
        inputs.property("version", project.property("otg_version"))
        inputs.property("minecraft_version", project.property("minecraft_version"))
        inputs.property("fabric_loader_version", project.property("fabric_loader_version"))

        filesMatching("fabric.mod.json") {
            val map = mapOf(
                "version" to project.property("otg_version").toString(),
                "minecraft_version" to project.property("minecraft_version").toString(),
                "fabric_loader_version" to project.property("fabric_loader_version").toString(),
            )
            expand(map)
        }
    }

    shadowJar {
        exclude("architectury.common.json")
        configurations = listOf(otg)
        archiveClassifier.set("deobf-all")
    }

    remapJar {
        //injectAccessWidener = true
        dependsOn(shadowJar)
        inputFile.set(shadowJar.get().archiveFile)

        archiveVersion = project.property("otg_version").toString()
    }

    sourcesJar {
        val commonSources = project(":common").tasks.sourcesJar
        //def sharedSources = project(":platforms:shared").sourcesJar
        //dependsOn commonSources, sharedSources
        from(commonSources.get().archiveFile.map { zipTree(it) })
        //from sharedSources.archiveFile.map { zipTree(it) }
    }
}

otgPlatform {
    productionJar.set(tasks.remapJar.flatMap { it.archiveFile })
}