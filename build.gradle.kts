plugins {
    kotlin("jvm") version "1.5.0"

    // Tests and code quality.
    id("io.gitlab.arturbosch.detekt") version "1.17.0"
}

group = "com.dynatrace.kached-properties"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.2")
    testImplementation("io.mockk:mockk:1.10.6")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.17.0")
}

configurations {
    all {
        // Excluded due to requirement for kotlinx-html which is still in JCenter.
        exclude("org.jetbrains.kotlinx", "kotlinx-html-jvm")
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

detekt {
    buildUponDefaultConfig = true
    config = files("$projectDir/detekt-custom-config.yml")
    reports {
        html.enabled = false // Disabled due to requirement for kotlinx-html which is still in JCenter.
    }
}
