plugins {
    `project-conventions`
    `maven-publish-conventions`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":channels-core"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlin.coroutines)
            }
        }
    }
}
