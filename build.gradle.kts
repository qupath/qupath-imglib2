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
    api("net.imglib2:imglib2:7.1.5")
    implementation("net.imglib2:imglib2-realtransform:4.0.3")

    // QuPath
    api("io.github.qupath:qupath-core:0.6.0")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")

    // Cache
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.2")

    // Unit tests
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }

    withSourcesJar()
    withJavadocJar()
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
