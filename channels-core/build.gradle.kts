plugins {
    `project-conventions`
    `maven-publish-conventions`
}

dependencies {
    implementation(libs.jctools)
    implementation(libs.atomicfu)
    implementation(libs.stately.collections)

    testImplementation(libs.bundles.junit)
    testImplementation(libs.bundles.kotest)
}
