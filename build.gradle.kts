val ktor_version: String by project
val logback_version: String by project
val kotlin_version: String by project
val kotlin_cli_version: String by project

plugins {
    application
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "3.0.0-rc-1"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.20"
}

group = "com.example.julianczaja"
version = "0.6.4"


application {
    mainClass.set("com.example.julianczaja.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

tasks {
    shadowJar {
        archiveFileName = "esp_monitoring_$version.jar"
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
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-freemarker:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("org.jetbrains.kotlinx:kotlinx-cli:$kotlin_cli_version")
    implementation("com.sksamuel.scrimage:scrimage-core:4.2.0")
}
