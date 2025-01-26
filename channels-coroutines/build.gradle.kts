plugins {
    `project-conventions`
    `maven-publish-conventions`
}

dependencies {
    api(project(":channels-core"))
    implementation(libs.kotlin.coroutines)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.bundles.kotest)
}
