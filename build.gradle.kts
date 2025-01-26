plugins {
    `project-conventions`
    `jacoco-report-aggregation`
    id("test-report-aggregation")
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
