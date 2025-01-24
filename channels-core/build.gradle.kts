plugins {
    kotlin("jvm") version "2.0.21"
}

group = "io.kriptal.channels"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jctools:jctools-core:4.0.5")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(11)
}