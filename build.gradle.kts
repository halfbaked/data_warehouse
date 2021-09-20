val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project
val koin_version: String by project

plugins {
    application
    kotlin("jvm") version "1.5.30"
    id("com.github.johnrengelman.shadow") version "7.0.0"
}

group = "com.erratick"
version = "0.0.1"
application {
    mainClass.set("com.erratick.ApplicationKt")
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
    maven { url = uri("https://kotlin.bintray.com/ktor") }
    jcenter()
}

dependencies {
    // Core dependencies
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")
    implementation("io.ktor:ktor-locations:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.3")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.12.1")

    // Koin
    implementation("org.koin:koin-core:$koin_version")
    implementation("org.koin:koin-ktor:$koin_version")
    implementation("org.koin:koin-core-ext:$koin_version")

    // Additional dependencies
    implementation("com.influxdb:influxdb-client-kotlin:3.3.0")

    // Test dependencies
    testImplementation("io.ktor:ktor-server-tests:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")
}

tasks{
    shadowJar {
        manifest {
            attributes(Pair("Main-Class", "com.erratick.ApplicationKt"))
        }
    }
}