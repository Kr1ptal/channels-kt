import org.jreleaser.model.Active

plugins {
    `project-conventions`
    `jacoco-report-aggregation`
    id("test-report-aggregation")
    alias(libs.plugins.jreleaser)
}

dependencies {
    // contains only submodules that are released
    val releasedSubmodules = listOf(
        ":channels-core",
        ":channels-coroutines",
    )

    releasedSubmodules.forEach {
        jacocoAggregation(project(it))
        testReportAggregation(project(it))
    }
}

// TODO, see: https://github.com/Kr1ptal/ethers-kt/issues/66
/*tasks.withType<Test> {
    finalizedBy(tasks.named<JacocoReport>("testCodeCoverageReport"))
}*/

tasks.check {
    dependsOn(tasks.named<TestReport>("testAggregateTestReport"))
}

allprojects {
    group = "io.kriptal.channels"
    version = "0.1.0-SNAPSHOT"
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
        description.set("High-performance Channel abstraction for Kotlin and JVM.")
        links {
            homepage.set("https://github.com/Kr1ptal/channels-kt")
        }
        license.set("Apache-2.0")
        inceptionYear.set("2025")
        authors.set(listOf("Kriptal"))
    }

    val stagingDir = layout.buildDirectory.dir("staging-deploy")

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
                }
            }
        }
    }
}
