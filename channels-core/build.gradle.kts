plugins {
    id("project-conventions")
    id("maven-publish-conventions")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.kotlin.coroutines)
                implementation(libs.stately.collections)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.coroutines.test)
            }
        }

        val jvmAndroidMain by getting {
            dependencies {
                implementation(libs.jctools)
            }
        }
    }
}
