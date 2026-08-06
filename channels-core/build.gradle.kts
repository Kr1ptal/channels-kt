plugins {
    id("com.android.library")
    `project-conventions`
    `maven-publish-conventions`
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.atomicfu)
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
