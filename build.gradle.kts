plugins {
    id("java-library")
    `maven-publish`
}

group = "io.github.qupath"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven {
        url = uri("https://maven.scijava.org/content/repositories/public/")
    }
}

dependencies {
    // ImgLib2
    api(sciJava.imglib2.imglib2)
    implementation(sciJava.imglib2.realtransform)

    // QuPath
    api(qupath.qupath.core)

    // Logging
    implementation(qupath.slf4j)

    // Cache - can later use QuPath catalog (added in v0.7.0)
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")

    // Unit tests
    testImplementation(qupath.junit)
    testImplementation(qupath.junit.platform)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(findToolchainVersion())
    }

    withSourcesJar()
    withJavadocJar()
}

/**
 * We may need to be able to override the version catalog Java with a system property
 * if we're including this build alongside QuPath's latest code.
 */
fun findToolchainVersion(): String {
    // Try System property
    val toolchainVersion = System.getProperty("toolchain")
    if (!toolchainVersion.isNullOrEmpty()) {
        return toolchainVersion
    }
    // Default to version catalog
    return qupath.versions.jdk.get()
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    repositories {
        maven {
            name = "SciJava"
            val releasesRepoUrl = uri("https://maven.scijava.org/content/repositories/releases")
            val snapshotsRepoUrl = uri("https://maven.scijava.org/content/repositories/snapshots")
            // Use gradle -Prelease publish
            url = if (project.hasProperty("release")) releasesRepoUrl else snapshotsRepoUrl
            credentials {
                username = System.getenv("MAVEN_USER")
                password = System.getenv("MAVEN_PASS")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            pom {
                licenses {
                    license {
                        name = "Apache License v2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0"
                    }
                }
            }
        }
    }
}
