plugins {
    id("project-conventions")
    id("maven-publish-conventions")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.stately.collections)
            }
        }

        val jvmAndroidMain by getting {
            dependencies {
                implementation(libs.jctools)
            }
        }
    }
}
