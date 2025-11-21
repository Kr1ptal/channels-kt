import org.jreleaser.gradle.plugin.dsl.deploy.maven.MavenDeployer
import org.jreleaser.model.Active

plugins {
    `project-conventions`
    alias(libs.plugins.jreleaser)
}

tasks.register("test", Test::class) {
    dependsOn(tasks.named("kotest"))
}

tasks.check {
    dependsOn(tasks.named("kotest"))
}

allprojects {
    group = "io.kriptal.channels"
    version = "1.0.0-SNAPSHOT"
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

    // Dynamically configure artifactOverride for all non-JVM KMP targets
    fun MavenDeployer.configureKmpOverrides() {
        rootProject.subprojects.forEach { subproject ->
            kotlin.targets.forEach { target ->
                // Skip JVM target (produces JAR, not klib)
                if (target.platformType.name != "jvm") {
                    val id = "${subproject.name}-${target.name.lowercase()}"
                    println("Configuring jreleaser artifactOverride for '$id'")

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
