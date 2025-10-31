plugins {
    alias(libs.plugins.shadowJar)
    alias(libs.plugins.kotlin.plugin.serialization)
    kotlin("jvm")
}

architectury {
    platformSetupLoomIde()
    fabric()
}

configurations {
    create("common")

    named("common") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    named("compileClasspath").configure {
        extendsFrom(configurations["common"])
    }
    named("runtimeClasspath").configure {
        extendsFrom(configurations["common"])
    }
    named("developmentFabric").configure {
        extendsFrom(configurations["common"])
    }

    create("shadowBundle")

    named("shadowBundle") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }
}

repositories {
    maven("https://jitpack.io")
}

dependencies {
    modImplementation(libs.architectury.fabric)
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    implementation(kotlin("stdlib"))
    "shadowBundle"(kotlin("stdlib"))

    include(libs.adventure.platform.fabric)
    modImplementation(libs.adventure.platform.fabric)

    implementation(libs.adventure.minimessage)
    "shadowBundle"(libs.adventure.minimessage)
    implementation(libs.adventure.legacy)
    "shadowBundle"(libs.adventure.legacy)
    implementation(libs.kotlin.serializationJson)
    "shadowBundle"(libs.kotlin.serializationJson)
    implementation(libs.okhttp)
    "shadowBundle"(libs.okhttp)
    implementation(libs.kotlinx.datetime)
    "shadowBundle"(libs.kotlinx.datetime)

    "common"(project(path = ":commonMod", configuration = "namedElements")) { isTransitive = false }
    "shadowBundle"(project(path = ":commonMod", configuration = "transformProductionFabric")) { isTransitive = false }
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to project.version))
    }
}

tasks.shadowJar {
    configurations = listOf(project.configurations.getByName("shadowBundle"))
    archiveClassifier.set("dev-shadow")
//    minimize()
    relocate("kotlinx", "com.wynnscribe.libs.kotlinx")
    relocate("kotlin", "com.wynnscribe.libs.kotlin")
    relocate("okhttp", "com.wynnscribe.libs.okhttp")
    relocate("okio", "com.wynnscribe.libs.okio")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}