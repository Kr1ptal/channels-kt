plugins {
    `maven-publish`
}

val configureMavenCentralRepo: Action<RepositoryHandler> = Action {
    // For JReleaser, we'll use a local staging directory
    // JReleaser will handle the actual upload to Maven Central
    maven {
        name = "localStaging"
        url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
    }

    // Always publish to local
    mavenLocal()
}

val configurePom = Action<MavenPom> {
    name = project.name
    description =
        "High-performance Channel abstraction for Kotlin and JVM."
    url = "https://github.com/Kr1ptal/channels-kt"

    licenses {
        license {
            name = "Apache-2.0 License"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            distribution = "repo"
        }
    }
    developers {
        developer {
            id = "kriptal"
            name = "Kriptal"
            organization = "Kriptal"
            organizationUrl = "https://kriptal.io"
        }
    }
    scm {
        url = "https://github.com/Kr1ptal/channels-kt"
        connection = "scm:git:git://github.com/Kr1ptal/channels-kt.git"
        developerConnection = "scm:git:ssh://git@github.com/Kr1ptal/channels-kt.git"
    }
}

project.pluginManager.withPlugin("java-platform") {
    publishing {
        publications {
            create<MavenPublication>("libraryBom") {
                pom(configurePom)
                from(components["javaPlatform"])
            }
        }

        repositories(configureMavenCentralRepo)
    }
}

project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
    // Apply Dokka plugin for documentation generation
    apply(plugin = "org.jetbrains.dokka")

    // Create javadoc jar from Dokka HTML output (Dokka V2 API)
    val dokkaGeneratePublicationHtml by tasks.getting

    val javadocJar by tasks.registering(Jar::class) {
        dependsOn(dokkaGeneratePublicationHtml)
        archiveClassifier.set("javadoc")
        from(layout.buildDirectory.dir("dokka/html"))
    }

    publishing {
        repositories(configureMavenCentralRepo)

        publications.withType<MavenPublication> {
            // Add javadoc jar to all publications
            artifact(javadocJar)
            pom(configurePom)
        }
    }
}
