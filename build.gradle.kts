plugins {
    kotlin("jvm") version "1.6.20"

    // Tests and code quality.
    id("io.gitlab.arturbosch.detekt") version "1.18.1"

    // Publishing.
    maven
    `maven-publish`
    signing
}

group = "com.dynatrace"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("io.mockk:mockk:1.12.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.19.0")
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

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "kached-properties"
            from(components["java"])

            pom {
                name.set("kached-properties")
                description.set("Caching in Kotlin made simple.")
                url.set("https://github.com/dynatrace-oss/kached-properties")

                licenses {
                    license {
                        name.set("Apache License, version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/dynatrace-oss/kached-properties.git/")
                    developerConnection.set("scm:git:ssh://github.com:dynatrace-oss/kached-properties.git")
                    url.set("https://github.com/dynatrace-oss/kached-properties.git")
                }

                developers {
                    developer {
                        id.set("krzema12")
                        name.set("Piotr Krzemiński")
                        email.set("piotr.krzeminski@dynatrace.com")
                    }
                    developer {
                        id.set("urielsalis")
                        name.set("Uriel Salischiker")
                        email.set("uriel.salischiker@dynatrace.com")
                    }
                }
            }
        }
    }

    val ossrhUsername: String? by project
    val ossrhPassword: String? by project

    repositories {
        maven(url = "https://oss.sonatype.org/service/local/staging/deploy/maven2/") {
            credentials {
                username = ossrhUsername
                password = ossrhPassword
            }
        }
    }
}

tasks {
    signing {
        sign(publishing.publications["mavenJava"])
    }
}

val validateVersionInReadme by tasks.creating<Task> {
    doLast {
        require(
            File("README.md").readText().contains("implementation(\"com.dynatrace:kached-properties:$version\")")
        ) { "Library versions stated in build.gradle.kts and in README.md should be equal!" }
    }
}

tasks.getByName("check").dependsOn(validateVersionInReadme)
