import io.gitlab.arturbosch.detekt.Detekt

plugins {
    kotlin("jvm") version "1.4.32"

    // Tests and code quality.
    id("io.gitlab.arturbosch.detekt") version "1.16.0"
}

group = "com.dynatrace.kached-properties"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()

    // This JCenter entry should be completely gone once JCenter is deprecated.
    // See https://jfrog.com/blog/into-the-sunset-bintray-jcenter-gocenter-and-chartcenter/
    jcenter {
        content {
            // Detekt needs 'kotlinx-html' for the HTML report.
            includeGroup("org.jetbrains.kotlinx")
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("io.mockk:mockk:1.10.6")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.16.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

// A workaround for https://github.com/detekt/detekt/issues/2956
tasks.getByName<Task>("check") {
    dependsOn("detektTest")
}
