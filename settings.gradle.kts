pluginManagement {
    repositories {
        gradlePluginPortal()
        maven {
            url = uri("https://maven.scijava.org/content/repositories/releases")
        }
    }
}

rootProject.name = "qupath-imglib2"

// Used for version catalogs (including Java compatibility)
val qupathVersion = "0.6.0"
val sciJavaVersion = "43.0.0"

dependencyResolutionManagement {

    versionCatalogs {
        create("sciJava") {
            from("org.scijava:pom-scijava:$sciJavaVersion")
        }
    }

    versionCatalogs {
        create("qupath") {
            from("io.github.qupath:qupath-catalog:$qupathVersion")
        }
    }

    repositories {
        maven("https://maven.scijava.org/content/groups/public/")
        mavenCentral()
    }

}
