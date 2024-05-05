plugins {
    id("java")
}

group = "pl.edu.agh.distributed.middleware.hw.iceserver"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.zeroc:ice:3.7.2")
}

tasks.test {
    useJUnitPlatform()
}

// sourceSets["main"].java.srcDir(file("generated"))