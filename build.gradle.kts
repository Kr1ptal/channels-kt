import org.jreleaser.gradle.plugin.dsl.deploy.maven.MavenDeployer
import org.jreleaser.model.Active

plugins {
    base
    idea
    alias(libs.plugins.jreleaser)
}

allprojects {
    group = "io.kriptal.channels"
    version = "1.0.5-SNAPSHOT"
}

jreleaser {
    signing {
        active.set(Active.ALWAYS)
        armored.set(true)
        verify.set(true)
    }

    // Configure release to skip GitHub operations since we only want Maven Central deployment
    release {
        github {
            enabled.set(true)
            skipRelease.set(true)
            skipTag.set(true)
            token.set("dummy") // Dummy token since no GitHub operations are performed
        }
    }

    // Set project info for deployment
    project {
        description.set("Kotlin Multiplatform channel-like abstraction over queues. Supports JVM and iOS targets")
        links {
            homepage.set("https://github.com/Kr1ptal/channels-kt")
        }
        license.set("Apache-2.0")
        inceptionYear.set("2025")
        authors.set(listOf("Kriptal"))
    }

    val stagingDir = layout.buildDirectory.dir("staging-deploy")

    fun MavenDeployer.configureKmpOverrides() {
        listOf(
            "channels-core-android",
            "channels-core-iosarm64",
            "channels-core-iossimulatorarm64",
            "channels-core-iosx64",
            "channels-core-macosarm64",
            "channels-coroutines-android",
            "channels-coroutines-iosarm64",
            "channels-coroutines-iossimulatorarm64",
            "channels-coroutines-iosx64",
            "channels-coroutines-macosarm64",
        ).forEach { id ->
            artifactOverride {
                groupId = "io.kriptal.channels"
                artifactId = id
                jar.set(false)
                sourceJar.set(false)
                javadocJar.set(false)
                verifyPom.set(false)
            }
        }
    }

    deploy {
        maven {
            mavenCentral {
                create("release-deploy") {
                    active.set(Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")

                    applyMavenCentralRules.set(true)
                    stagingRepository(stagingDir.get().asFile.absolutePath)

                    username.set(System.getenv("MAVEN_CENTRAL_USERNAME"))
                    password.set(System.getenv("MAVEN_CENTRAL_PASSWORD"))

                    configureKmpOverrides()
                }
            }

            nexus2 {
                create("snapshot-deploy") {
                    active.set(Active.SNAPSHOT)

                    url.set("https://central.sonatype.com/repository/maven-snapshots/")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots/")

                    applyMavenCentralRules.set(true)
                    snapshotSupported.set(true)
                    closeRepository.set(true)
                    releaseRepository.set(true)
                    stagingRepository(stagingDir.get().asFile.absolutePath)

                    username.set(System.getenv("MAVEN_CENTRAL_USERNAME"))
                    password.set(System.getenv("MAVEN_CENTRAL_PASSWORD"))

                    configureKmpOverrides()
                }
            }
        }
    }
}
