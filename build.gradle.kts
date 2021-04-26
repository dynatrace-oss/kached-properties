plugins {
    kotlin("jvm") version "1.4.32"
}

group = "com.dynatrace.kached-properties"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("io.mockk:mockk:1.10.6")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
