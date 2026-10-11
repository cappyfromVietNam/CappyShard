plugins {
    java
    id("com.gradleup.shadow") version "8.3.7"
}

group = "com.cappy.shards"
version = "1.0.0"
description = "CappyShards - Advanced Shards System for Paper Minecraft Servers"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
}

dependencies {
    // Paper API (1.21+)
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")

    // PlaceholderAPI
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Annotations
    compileOnly("org.jetbrains:annotations:24.1.0")

    // Database
    implementation("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.xerial:sqlite-jdbc:3.46.1.0")
    implementation("org.slf4j:slf4j-nop:1.7.36")

    // bStats Metrics
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    archiveFileName.set("CappyShards-${project.version}.jar")
    relocate("org.bstats", "com.cappy.shards.bstats")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
