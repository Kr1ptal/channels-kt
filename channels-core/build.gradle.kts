plugins {
    `project-conventions`
    `maven-publish-conventions`
}

dependencies {
    implementation(libs.jctools)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.bundles.kotest)
}
