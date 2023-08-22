val ktor_version: String by project
val logback_version: String by project

plugins {
    application
    kotlin("jvm") version "1.8.20"
    id("io.ktor.plugin") version "2.3.3"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.8.20"
}

group = "com.example.julianczaja"
version = "0.2.1"


application {
    mainClass.set("com.example.julianczaja.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

tasks {
    shadowJar {
        archiveName = "esp_monitoring_$version.jar"
        manifest {
            attributes(Pair("Main-Class", "com.example.ApplicationKt"))
            mergeServiceFiles()
        }
    }
}

dependencies {
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktor_version")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
}